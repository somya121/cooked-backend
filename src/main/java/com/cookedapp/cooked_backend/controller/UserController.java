package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.CookProfileDTO;
import com.cookedapp.cooked_backend.dto.MessageResponse;
import com.cookedapp.cooked_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For method security (optional)
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // To get current user
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.service.AuthService;
import com.cookedapp.cooked_backend.service.JwtService;
import com.cookedapp.cooked_backend.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import com.cookedapp.cooked_backend.dto.CookProfileSetupRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users") // Base path for user-related actions
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600) // Adjust origin policy
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    @PutMapping("/profile-setup")
    public ResponseEntity<?> setupCookProfile(@Valid @RequestBody CookProfileSetupRequest setupRequest) { // Create this DTO
        logger.info("Attempting to complete profile using setup token.");
        try {
            // Call the service method to validate token, update profile, and activate user
            User activatedUser = authService.completeCookProfile(
                    setupRequest.getSetupToken(),
                    setupRequest.getProfileData() // Pass the nested profile DTO
            );
            logger.info("Profile setup successful for user: {}", activatedUser.getUsername());

            // Profile complete, now generate the real JWT for login
            String authToken = jwtService.generateToken(activatedUser);
            List<String> rolesList = activatedUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            // Return the standard AuthResponse to log the user in
            return ResponseEntity.ok(new AuthResponse(
                    "Profile setup successful. Logged in.",
                    activatedUser.getUsername(),
                    authToken,
                    rolesList
            ));

        } catch (IllegalArgumentException e) { // Catch invalid/expired token errors
            logger.warn("Profile setup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error completing cook profile setup: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error completing profile setup: " + e.getMessage()));
        }
    }


    @PutMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody CookProfileDTO profileDto
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(new MessageResponse("User not authenticated"));
        }
        String identifier = currentUser.getUsername();
        logger.info("Updating profile for user: {}", identifier);
        try {
            userService.updateUserProfile(identifier, profileDto);
            return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
        } catch (UsernameNotFoundException e) {
            logger.warn("Profile update failed: User not found - {}", identifier);
            return ResponseEntity.status(404).body(new MessageResponse("User not found."));
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: ", identifier, e);
            return ResponseEntity.status(500).body(new MessageResponse("Error updating profile: " + e.getMessage()));
        }
    }

    // Optional: Separate endpoint for profile picture upload

    @PostMapping("/me/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam("image") MultipartFile imageFile) {
        if (currentUser == null) { return ResponseEntity.status(401).build(); }
        if (imageFile.isEmpty()) { return ResponseEntity.badRequest().build(); }

        String identifier = currentUser.getUsername();
        logger.info("Uploading profile picture for user: {}", identifier);
        try {
            // Assuming service method handles storage and returns updated user or just URL
            userService.updateUserProfilePicture(identifier, imageFile);
            return ResponseEntity.ok(new MessageResponse("Profile picture updated successfully!"));
        } catch (Exception e) {
             logger.error("Error uploading profile picture for user {}: ", identifier, e);
            return ResponseEntity.status(500).body(new MessageResponse("Error uploading picture: " + e.getMessage()));
        }
    }

}