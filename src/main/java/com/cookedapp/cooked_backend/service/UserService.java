package com.cookedapp.cooked_backend.service;

import com.cookedapp.cooked_backend.controller.UserController;
import com.cookedapp.cooked_backend.dto.CookProfileDTO;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Value("${app.upload.dir:${user.home}/cooked_app_uploads/images/profiles}")
    private String uploadDir;
    @Transactional
    public User updateUserProfile(String email, CookProfileDTO profileDto) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + email));

        // Update fields from DTO
        user.setChargesPerMeal(profileDto.getChargesPerMeal());
        user.setCookname(profileDto.getCookname());
        user.setPhone(profileDto.getPhone());
        user.setAvailabilityStatus(profileDto.getAvailabilityStatus());
        user.setLatitude(profileDto.getLatitude());
        user.setLongitude(profileDto.getLongitude());
        if (profileDto.getLatitude() != null && profileDto.getLongitude() != null) {
            user.setLatitude(profileDto.getLatitude());
            user.setLongitude(profileDto.getLongitude());
            try {
                String placeName = geocodingService.getPlaceName(profileDto.getLatitude(), profileDto.getLongitude());
                user.setPlaceName(placeName);
                logger.info("Updated place name for user {}: {}", email, placeName);
            } catch (Exception e) {
                logger.error("Failed to get place name during profile update for user {}: {}", email, e.getMessage());
                user.setPlaceName(null);
            }
        } else {
            user.setLatitude(null);
            user.setLongitude(null);
            user.setPlaceName(null);
        }

        if (profileDto.getExpertise() != null) {
            user.setExpertise(profileDto.getExpertise());
        }
        user.setStatus("ACTIVE");
        if (!user.getRoles().contains("ROLE_COOK")) {

            user.setRoles(user.getRoles() + ",ROLE_COOK");
        }
        logger.info("User {} profile completed. Status set to ACTIVE, ROLE_COOK assigned.", user.getUsername());

        return userRepository.save(user);
    }

    public User getUserByEmail(String email){
        logger.info("UserService: Attempting to find user by email: [{}]", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("UserService: User NOT FOUND with email: [{}]", email);
                    return new UsernameNotFoundException("User not found with email: " + email); // Make sure exception uses the email
                });
    }

    @Transactional
    public User updateUserProfilePicture(String usernameOrEmail, MultipartFile imageFile) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));
        if (imageFile.isEmpty() || imageFile.getOriginalFilename() == null || imageFile.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("Image file is empty or invalid.");
        }
        String originalFilename = imageFile.getOriginalFilename();
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }
        String filename = "user_" + user.getId() + "_" + System.currentTimeMillis() + extension;
        try {
            Path staticResourcePath = Paths.get("src", "main", "resources", "static", "images", "profiles");
            if (!Files.exists(staticResourcePath)) {
                Files.createDirectories(staticResourcePath);
                logger.info("Created directory for images: {}", staticResourcePath.toAbsolutePath().toString());
            }

            Path destinationFile = staticResourcePath.resolve(filename);
            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Successfully saved profile picture to: {}", destinationFile.toAbsolutePath().toString());
            }
            String imageUrl = "/images/profiles/" + filename;
            user.setProfilePicture(imageUrl);
            return userRepository.save(user);
        } catch (Exception e) {
            logger.error("Failed to store profile picture", e);
            throw new RuntimeException("Could not store profile picture. Please try again.", e);
        }

    }

    public List<CookProfileDTO> findNearbyCooks(double latitude, double longitude, double radiusKm) {
        logger.info("Finding cooks near lat: {}, lon: {}, radius: {}km", latitude, longitude, radiusKm);
        try {
            List<User> nearbyCookEntities = userRepository.findActiveCooksNearbyNativeWithDistance(latitude, longitude, radiusKm);
            logger.info("UserService: userRepository.findActiveCooksNearbyNativeWithDistance returned {} entities.", nearbyCookEntities.size());

            List<CookProfileDTO> dtos = nearbyCookEntities.stream()
                    .map(this::convertToCookDTO)
                    .collect(Collectors.toList());
            logger.info("UserService: Converted to {} DTOs.", dtos.size());
            return dtos;
        } catch (Exception e) {
            logger.error("UserService: Error in findNearbyCooks: {}", e.getMessage(), e);
            throw e;
        }
    }

    private CookProfileDTO convertToCookDTO(User user) {

        return CookProfileDTO.builder()
                .id(user.getId())
                .cookname(user.getCookname())
                .phone(user.getPhone())
                .expertise(user.getExpertise())
                .availabilityStatus(user.getAvailabilityStatus())
                .profilePicture(user.getProfilePicture())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .chargesPerMeal(user.getChargesPerMeal())
                .averageRating(user.getAverageRating())
                .numberOfRatings(user.getNumberOfRatings())
                .placeName(user.getPlaceName())
                // .distanceKm(user.getDistance())
                .build();
    }
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in km
    }

}