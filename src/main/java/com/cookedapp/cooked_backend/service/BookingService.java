package com.cookedapp.cooked_backend.service;

import com.cookedapp.cooked_backend.dto.BookingRequestDTO;
import com.cookedapp.cooked_backend.dto.BookingResponseDTO;
import com.cookedapp.cooked_backend.dto.NotificationPayload;
import com.cookedapp.cooked_backend.entity.Booking;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RatingRepository ratingRepository;

    private User getUserFromDetails(UserDetails userDetails) {
        if (userDetails instanceof com.cookedapp.cooked_backend.entity.User) {

            String email = ((com.cookedapp.cooked_backend.entity.User) userDetails).getEmail();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email from UserDetails: " + email));
        } else {
                       String emailFromPrincipal = userDetails.getUsername();
            return userRepository.findByEmail(emailFromPrincipal)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email from generic UserDetails principal: " + emailFromPrincipal));
        }
    }

    @Transactional
    public void deleteBooking(Long bookingId, UserDetails currentUserDetails) {
        User customer = getUserFromDetails(currentUserDetails);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException("You are not authorized to delete this booking.");
        }

        if ("COMPLETED".equals(booking.getBookingStatus()) || "REJECTED".equals(booking.getBookingStatus())) {
            throw new IllegalStateException("Cannot delete a booking that is already " + booking.getBookingStatus().toLowerCase() + ".");
        }

        if ("ACCEPTED".equals(booking.getBookingStatus())) {
            User cook = booking.getCook();
            String cookMessage = String.format("Booking ID %d with customer %s has been cancelled by the customer.",
                    booking.getId(), customer.getUsername());
            NotificationPayload payload = new NotificationPayload(
                    cookMessage,
                    booking.getId(),
                    "BOOKING_CANCELLED_BY_USER",
                    LocalDateTime.now(),
                    customer.getId(),
                    cook.getId()
            );
            notificationService.sendNotificationToCook(cook.getId(), payload);
            logger.info("Notified cook {} about cancellation of booking ID {}", cook.getUsername(), booking.getId());
        }
        bookingRepository.delete(booking);
        logger.info("Booking ID: {} deleted by customer {}", bookingId, customer.getUsername());
    }





    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO requestDTO, UserDetails currentUserDetails) {
        User customer = getUserFromDetails(currentUserDetails);
        User cook = userRepository.findById(requestDTO.getCookId())
                .filter(c -> c.getRoles().contains("ROLE_COOK") && "ACTIVE".equals(c.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Cook not found or not active with ID: " + requestDTO.getCookId()));

        String details = String.format("Name: %s, Address: %s, Meal Preference: %s",
                requestDTO.getCustomerName(), requestDTO.getCustomerAddress(), requestDTO.getMealPreference());

        Booking booking = Booking.builder()
                .customer(customer)
                .cook(cook)
                .bookingDetails(details)
                .bookingStatus("PENDING")
                .requestedDateTime(requestDTO.getRequestedDateTime())
                .build();
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking created with ID: {} by customer {} for cook {}", savedBooking.getId(), customer.getUsername(), cook.getUsername());


        NotificationPayload notifPayload = new NotificationPayload(
                "You have a new booking request from " + customer.getUsername(),
                savedBooking.getId(),
                "NEW_BOOKING_REQUEST",
                null,
                customer.getId(),
                cook.getId() 
        );
        notificationService.sendNotificationToCook(cook.getId(), notifPayload);

        return convertToBookingResponseDTO(savedBooking);
    }

    @Transactional
    public BookingResponseDTO updateBookingStatus(Long bookingId, String newStatus, UserDetails currentCookDetails) {
        User cook = getUserFromDetails(currentCookDetails);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));
        if (!booking.getCook().getId().equals(cook.getId())) {
            throw new IllegalStateException("You are not authorized to update this booking.");
        }

        if (!List.of("ACCEPTED", "REJECTED").contains(newStatus.toUpperCase())) {
            throw new IllegalArgumentException("Invalid status update: " + newStatus);
        }
        if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
            if (booking.getCook().getChargesPerMeal() != null) {
                booking.setTotalCharges(booking.getCook().getChargesPerMeal());
            } else {
                logger.warn("Booking ID {} accepted but cook {} has no chargesPerMeal set.", booking.getId(), cook.getUsername());
                booking.setTotalCharges(0.0);
            }
        }
        booking.setBookingStatus(newStatus.toUpperCase());
        Booking updatedBooking = bookingRepository.save(booking);
        logger.info("Booking ID: {} status updated to {} by cook {}", updatedBooking.getId(), newStatus, cook.getUsername());

        String customerMessage = String.format("Your booking request (ID: %d) with cook %s has been %s.",
                updatedBooking.getId(), cook.getUsername(), newStatus.toLowerCase());
        NotificationPayload notifPayload = new NotificationPayload(
                customerMessage,
                updatedBooking.getId(),
                "BOOKING_" + newStatus.toUpperCase(),
                null,
                cook.getId(),
                booking.getCustomer().getId()
        );
        notificationService.sendNotificationToUser(booking.getCustomer().getId(), notifPayload);

        return convertToBookingResponseDTO(updatedBooking);
    }

    public List<BookingResponseDTO> getCookBookings(UserDetails cookUserDetails) {
        User cook = getUserFromDetails(cookUserDetails);
        return bookingRepository.findByCook(cook).stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getCustomerBookings(UserDetails customerUserDetails) {
        User customer = getUserFromDetails(customerUserDetails);
        return bookingRepository.findByCustomer(customer).stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public BookingResponseDTO markServiceAsComplete(Long bookingId, UserDetails currentCookDetails) {
        User cook = getUserFromDetails(currentCookDetails);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        if (!booking.getCook().getId().equals(cook.getId())) {
            throw new IllegalStateException("You are not authorized to update this booking.");
        }
        if (!"ACCEPTED".equals(booking.getBookingStatus())) {
            throw new IllegalStateException("Booking must be in ACCEPTED status to mark service as complete.");
        }
        if (booking.getServiceCompletedAt() != null) {
            throw new IllegalStateException("Service for this booking has already been marked as complete.");
        }
        if (booking.getTotalCharges() == null) {

            if (cook.getChargesPerMeal() != null) {
                booking.setTotalCharges(cook.getChargesPerMeal());
            } else {
                booking.setTotalCharges(0.0);
                logger.warn("Marking service complete for booking {} but cook {} has no chargesPerMeal set recently.", bookingId, cook.getUsername());
            }
        }

        booking.setServiceCompletedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Service for Booking ID: {} marked as complete by cook {}", savedBooking.getId(), cook.getUsername());

        String userMessage = String.format("Service for your booking (ID: %d) with %s is complete. Please pay ₹%.2f.",
                savedBooking.getId(), cook.getCookname(), savedBooking.getTotalCharges());
        NotificationPayload notifPayload = new NotificationPayload(

                userMessage,
                savedBooking.getId(),
                "SERVICE_COMPLETED_PAYMENT_DUE",
                LocalDateTime.now(),
                cook.getId(),
                booking.getCustomer().getId()
        );
        notificationService.sendNotificationToUser(booking.getCustomer().getId(), notifPayload);

        return convertToBookingResponseDTO(savedBooking);
    }

    @Transactional
    public BookingResponseDTO markPaymentAsReceived(Long bookingId, UserDetails currentCookDetails) {
        User cook = getUserFromDetails(currentCookDetails);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        if (!booking.getCook().getId().equals(cook.getId())) {
            throw new IllegalStateException("You are not authorized to update this booking.");
        }
        if (booking.getServiceCompletedAt() == null) {
            throw new IllegalStateException("Service must be marked as complete before marking payment as received.");
        }
        if (booking.getPaymentCompletedAt() != null) {
            throw new IllegalStateException("Payment for this booking has already been marked as received.");
        }

        booking.setPaymentCompletedAt(LocalDateTime.now());
        booking.setBookingStatus("COMPLETED");
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Payment for Booking ID: {} marked as received by cook {}. Status set to COMPLETED.", savedBooking.getId(), cook.getUsername());

        String userMessage = String.format("Payment for booking (ID: %d) with %s confirmed. Thank you! Please consider rating your experience.",
                savedBooking.getId(), cook.getCookname());
        NotificationPayload notifPayload = new NotificationPayload(
                userMessage,
                savedBooking.getId(),
                "PAYMENT_COMPLETED_RATE_SERVICE",
                LocalDateTime.now(),
                cook.getId(),
                booking.getCustomer().getId()
        );
        notificationService.sendNotificationToUser(booking.getCustomer().getId(), notifPayload);
        return convertToBookingResponseDTO(savedBooking);
    }
    private BookingResponseDTO convertToBookingResponseDTO(Booking booking) {

        boolean isRated = ratingRepository.existsByBookingIdAndRatedByUserId(booking.getId(), booking.getCustomer().getId());
        String name = "", address = "", mealPref = "";
        if (booking.getBookingDetails() != null) {
            String[] parts = booking.getBookingDetails().split(", ");
            for (String part : parts) {
                if (part.startsWith("Name: ")) name = part.substring(6);
                else if (part.startsWith("Address: ")) address = part.substring(9);
                else if (part.startsWith("Meal Preference: ")) mealPref = part.substring(17);
            }
        }

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .customerUsername(booking.getCustomer().getUsername())
                .cookId(booking.getCook().getId())
                .cookUsername(booking.getCook().getUsername())
                .bookingDetails(booking.getBookingDetails())
                .customerName(name)
                .customerAddress(address)
                .mealPreference(mealPref)
                .bookingStatus(booking.getBookingStatus())
                .requestedDateTime(booking.getRequestedDateTime())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .totalCharges(booking.getTotalCharges())
                .serviceCompletedAt(booking.getServiceCompletedAt())
                .paymentCompletedAt(booking.getPaymentCompletedAt())
                .ratedByCurrentUser(isRated)
                .build();
    }
}