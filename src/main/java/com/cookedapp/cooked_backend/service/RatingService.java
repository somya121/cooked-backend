package com.cookedapp.cooked_backend.service;

import com.cookedapp.cooked_backend.dto.NotificationPayload;
import com.cookedapp.cooked_backend.dto.RatingRequestDTO;
import com.cookedapp.cooked_backend.dto.RatingResponseDTO;
import com.cookedapp.cooked_backend.entity.Booking;
import com.cookedapp.cooked_backend.entity.Rating;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.repository.BookingRepository;
import com.cookedapp.cooked_backend.repository.RatingRepository;
import com.cookedapp.cooked_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private User getUserFromDetails(UserDetails userDetails) {
        String identifier;
        if(userDetails instanceof User){
            User user = (User) userDetails;
            identifier = user.getEmail();
            logger.info("RatingService.getUserFromDetails: Principal is custom User. Using Email: [{}]. (Username field was: [{}])", identifier, user.getUsername());
        }
        else{
            identifier = userDetails.getUsername();
            logger.warn("RatingService.getUserFromDetails: Principal is NOT custom User (Type: {}). Falling back to UserDetails.getUsername(), assumed to be email: [{}]",
                    userDetails.getClass().getName(), identifier);
        }
        if (identifier == null || !identifier.contains("@")) {
            logger.error("RatingService.getUserFromDetails: CRITICAL - Identifier [{}] does not look like an email. Original UserDetails.getUsername() was [{}].", identifier, userDetails.getUsername());
            throw new UsernameNotFoundException("Could not determine a valid email identifier from UserDetails for user: " + userDetails.getUsername());
        }
        return userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found from UserDetails: " + identifier));
    }

    @Transactional
    public RatingResponseDTO submitRating(RatingRequestDTO ratingRequest, UserDetails currentUserDetails) {
        User customer = getUserFromDetails(currentUserDetails);
        Booking booking = bookingRepository.findById(ratingRequest.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + ratingRequest.getBookingId()));

        // Validations
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException("You can only rate bookings you made.");
        }
        if (booking.getPaymentCompletedAt() == null) {
            throw new IllegalStateException("Booking payment must be completed before rating.");
        }
        if (!"COMPLETED".equals(booking.getBookingStatus())) {
            throw new IllegalStateException("Booking must be in COMPLETED status to be rated.");
        }
        if (ratingRepository.existsByBookingIdAndRatedByUserId(booking.getId(), customer.getId())) {
            throw new IllegalStateException("You have already rated this booking.");
        }

        User cook = booking.getCook();

        Rating rating = Rating.builder()
                .booking(booking)
                .ratedByUser(customer)
                .ratedCook(cook)
                .ratingValue(ratingRequest.getRatingValue())
                .comment(ratingRequest.getComment())
                .build();
        Rating savedRating = ratingRepository.save(rating);
        logger.info("Rating ID {} submitted by user {} for cook {} on booking {}",
                savedRating.getId(), customer.getUsername(), cook.getUsername(), booking.getId());

        updateCookAverageRating(cook);

        // Notify cook
        String cookMessage = String.format("%s has rated your service for booking ID %d: %d stars.",
                customer.getUsername(), booking.getId(), rating.getRatingValue());
        NotificationPayload payload = new NotificationPayload(
                cookMessage,
                booking.getId(),
                "NEW_RATING_RECEIVED",
                LocalDateTime.now(),
                customer.getId(),
                cook.getId()
        );
        notificationService.sendNotificationToCook(cook.getId(), payload);

        return convertToResponseDTO(savedRating);
    }

    @Transactional
    public void updateCookAverageRating(User cook) {
        List<Rating> ratingsForCook = ratingRepository.findByRatedCook(cook);
        if (ratingsForCook.isEmpty()) {
            cook.setAverageRating(0.0);
            cook.setNumberOfRatings(0);
        } else {
            double sum = ratingsForCook.stream().mapToInt(Rating::getRatingValue).sum();
            double calculatedAverage = sum / ratingsForCook.size();
            double roundedAverage = Math.round(calculatedAverage * 10.0) / 10.0;
            cook.setAverageRating(roundedAverage);
            cook.setNumberOfRatings(ratingsForCook.size());
        }
        userRepository.save(cook);
        logger.info("Updated average rating for cook {}: Avg {}, Count {}",
                cook.getUsername(), cook.getAverageRating(), cook.getNumberOfRatings());
    }

    private RatingResponseDTO convertToResponseDTO(Rating rating) {
        return RatingResponseDTO.builder()
                .id(rating.getId())
                .bookingId(rating.getBooking().getId())
                .ratedByUserId(rating.getRatedByUser().getId())
                .ratedByUsername(rating.getRatedByUser().getUsername())
                .ratedCookId(rating.getRatedCook().getId())
                .ratedCookUsername(rating.getRatedCook().getUsername())
                .ratingValue(rating.getRatingValue())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    public List<RatingResponseDTO> getRatingsForCook(Long cookId) {
        User cook = userRepository.findById(cookId)
                .orElseThrow(() -> new UsernameNotFoundException("Cook not found with ID: " + cookId));
        return ratingRepository.findByRatedCook(cook).stream()
                .map(this::convertToResponseDTO)
                .toList();
    }
}