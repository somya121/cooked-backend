package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.*;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.service.AuthService;
import com.cookedapp.cooked_backend.service.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cookedapp.cooked_backend.dto.RegistrationInitResponse;
import org.springframework.security.core.AuthenticationException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;

    @Autowired
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/check-identifier")
    public ResponseEntity<?> checkUserExistence(@Valid @RequestBody IdentifierRequest request) {
        try {
            logger.info("Checking existence for identifier: {}", request.getIdentifier());
            boolean emailExists = authService.checkIdentifierExists(request.getIdentifier());
            boolean userNameExists = authService.checkIdentifierExists(request.getIdentifier());
            logger.info("Identifier exists: {}", emailExists);
            return ResponseEntity.ok(new IdentifierCheckResponse(emailExists, userNameExists,request.getIdentifier(),request.getIdentifier()));
        } catch (Exception e) {
            logger.error("Error checking identifier existence: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error checking identifier: " + e.getMessage()));
        }
    }


    @PostMapping("/register/user")
    public ResponseEntity<?> registerStandardUser(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Attempting standard user registration for username: {}", registerRequest.getUsername());
        try {
            AuthResponse response = authService.registerStandardUser(registerRequest);
            logger.info("Standard user registration successful for username: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Standard user registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during standard user registration:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Registration failed due to server error."));
        }
    }

    // POST /api/auth/register/cook
    @PostMapping("/register/cook")
    public ResponseEntity<?> registerCookInitiate(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Attempting cook registration initiation for username: {}", registerRequest.getUsername());
        try {
            AuthResponse response = authService.registerCookInitiate(registerRequest); // Service returns AuthResponse now
            logger.info("Cook registration initiation successful for username: {}", registerRequest.getUsername());
            // Return AuthResponse containing token needed for profile setup
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Cook registration initiation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during cook registration initiation:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Registration failed due to server error."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Attempting login for: {}", loginRequest.getIdentifier());

        try{
            AuthResponse authResponse = authService.loginUser(loginRequest);
            logger.info("Login Successful for user: {}",authResponse.getUsername());
            return ResponseEntity.ok(authResponse);
        }
        catch (AuthenticationException e){
            logger.warn("Login failed for email {}: {}", loginRequest.getIdentifier(), e.getMessage());
            // Return standard unauthorized response
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Login failed: Invalid email or password."));
        }
        catch (IllegalStateException e) {
            logger.warn("Login failed for email {}: {}", loginRequest.getIdentifier(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        }
        catch (Exception e) { // Catch any other unexpected server errors
            logger.error("An unexpected error occurred during login for email {}: ", loginRequest.getIdentifier(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred during login."));
        }
    }
}