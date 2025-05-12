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


@Service
@RequiredArgsConstructor
public class UserService { // Or name it CookService if specific to cooks

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Transactional // Make sure update is atomic
    public User updateUserProfile(String usernameOrEmail, CookProfileDTO profileDto) {
        // Find user by username or email depending on what your UserDetails provides
        User user = userRepository.findByEmail(usernameOrEmail) // Assuming login via email
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));

        // Update fields from DTO
        user.setCookname(profileDto.getCookname());
        user.setPhone(profileDto.getPhone());
        user.setAvailabilityStatus(profileDto.getAvailabilityStatus());
        user.setLatitude(profileDto.getLatitude());
        user.setLongitude(profileDto.getLongitude());


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

    @Transactional
    public User updateUserProfilePicture(String usernameOrEmail, MultipartFile imageFile) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));
        String filename = "user_" + user.getId() + "_" + System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
        String storagePath = "C:/Users/pravi/OneDrive/Pictures/Camera Roll" + filename;
        try {
            logger.info("Placeholder: Would save file to {}", storagePath);
        } catch (Exception e) {
            logger.error("Failed to store profile picture", e);
            throw new RuntimeException("Could not store profile picture. Please try again.", e); // Or custom exception
        }
        String imageUrl = "/images/profiles/" + filename;
        user.setProfilePicture(imageUrl);
        return userRepository.save(user);


    }

}