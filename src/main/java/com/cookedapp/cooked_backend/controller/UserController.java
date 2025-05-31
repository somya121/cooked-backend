package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.CookProfileDTO;
import com.cookedapp.cooked_backend.dto.MessageResponse;
import com.cookedapp.cooked_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    @PutMapping("/profile-setup")
    public ResponseEntity<?> setupCookProfile(@Valid @RequestBody CookProfileSetupRequest setupRequest) {
        logger.info("Attempting to complete profile using setup token.");
        try {
            User activatedUser = authService.completeCookProfile(
                    setupRequest.getSetupToken(),
                    setupRequest.getProfileData()
            );
            logger.info("Profile setup successful for user: {}", activatedUser.getUsername());

            String authToken = jwtService.generateToken(activatedUser);
            List<String> rolesList = activatedUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new AuthResponse(
                    activatedUser.getId(),
                    "Profile setup successful. Logged in.",
                    activatedUser.getUsername(),
                    authToken,
                    rolesList,
                    activatedUser.getStatus(),
                    activatedUser.getAverageRating(),
                    activatedUser.getNumberOfRatings()
            ));

        } catch (IllegalArgumentException e) {
            logger.warn("Profile setup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error completing cook profile setup: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error completing profile setup: " + e.getMessage()));
        }
    }
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal User currentUserDetails) {
        if (currentUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        logger.info("UserController.getMyProfile: Attempting to fetch profile for UserDetails.getUsername(): [{}]",currentUserDetails.getEmail(), currentUserDetails.getUsername());
        try {

            CookProfileDTO profileDto = CookProfileDTO.builder()
                    .id(currentUserDetails.getId())
                    .cookname(currentUserDetails.getCookname())
                    .phone(currentUserDetails.getPhone())
                    .expertise(currentUserDetails.getExpertise())
                    .availabilityStatus(currentUserDetails.getAvailabilityStatus())
                    .profilePicture(currentUserDetails.getProfilePicture())
                    .latitude(currentUserDetails.getLatitude())
                    .longitude(currentUserDetails.getLongitude())
                    .build();

            logger.info("Fetched profile for user: {}", currentUserDetails.getEmail());
            return ResponseEntity.ok(profileDto);
        } catch (UsernameNotFoundException e) {
            logger.warn("Profile fetch failed: User not found - {}", currentUserDetails.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User profile not found."));
        } catch (Exception e) {
            logger.error("Error fetching profile for user {}: ", currentUserDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error fetching profile: " + e.getMessage()));
        }
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CookProfileDTO profileDto
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(new MessageResponse("User not authenticated"));
        }
        String identifier = currentUser.getUsername();
        logger.info("Updating profile for user: {}", identifier);
        String emailToUpdate;
        if (currentUser instanceof User) {
            emailToUpdate = ((User) currentUser).getEmail();
        } else {
            logger.error("Authenticated principal is not an instance of User entity. Cannot get email.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Internal server error processing user identity."));
        }

        logger.info("Updating profile for user with email: {}", emailToUpdate);
        try {
            userService.updateUserProfile(emailToUpdate, profileDto);
            return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
        } catch (UsernameNotFoundException e) {
            logger.warn("Profile update failed: User not found - {}", emailToUpdate);
            return ResponseEntity.status(404).body(new MessageResponse("User not found."));
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: ", emailToUpdate, e);
            return ResponseEntity.status(500).body(new MessageResponse("Error updating profile: " + e.getMessage()));
        }
    }



    @PostMapping("/me/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam("image") MultipartFile imageFile) {
        if (currentUser == null) { return ResponseEntity.status(401).build(); }
        if (imageFile.isEmpty()) { return ResponseEntity.badRequest().build(); }

        String identifier ;
        if (currentUser instanceof User) {
            User customUser = (User) currentUser;
            identifier = customUser.getEmail();
            logger.info("UserController: Principal is instance of custom User. Email: {}, Username: {}", customUser.getEmail(), customUser.getUsername());

        } else {
            identifier = currentUser.getUsername();
            logger.warn("UserController (uploadProfilePicture): Principal is NOT custom User (Type: {}). Falling back to UserDetails.getUsername() which is assumed to be email: [{}]",
                    currentUser.getClass().getName(), identifier);
        }

        try {
            userService.updateUserProfilePicture(identifier, imageFile);
            return ResponseEntity.ok(new MessageResponse("Profile picture updated successfully!"));
        } catch (Exception e) {
             logger.error("Error uploading profile picture for user {}: ", identifier, e);
            return ResponseEntity.status(500).body(new MessageResponse("Error uploading picture: " + e.getMessage()));
        }
    }
    @GetMapping("/nearby")
    public ResponseEntity<List<CookProfileDTO>> getNearbyCooks(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "4") double radius) {

        try {
            List<CookProfileDTO> cooks = userService.findNearbyCooks(lat, lon, radius);
            logger.info("UserController: userService.findNearbyCooks returned {} cooks.", cooks.size());
            return ResponseEntity.ok(cooks);
        } catch (Exception e) {
            logger.error("UserController: Error in getNearbyCooks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}