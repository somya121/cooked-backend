package com.cookedapp.cooked_backend.service;

import com.cookedapp.cooked_backend.dto.LoginRequest;
import com.cookedapp.cooked_backend.dto.RegisterRequest;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cookedapp.cooked_backend.dto.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.cookedapp.cooked_backend.dto.RegistrationInitResponse;
import com.cookedapp.cooked_backend.dto.CookProfileDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;

@Service

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,AuthenticationManager authenticationManager,JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public boolean checkIdentifierExists(String identifier) {
        return userRepository.existsByEmail(identifier);
    }
public  boolean checkUsernameExists(String username){
        return userRepository.existsByUsername(username);
}

    @Transactional
    public AuthResponse registerStandardUser(RegisterRequest registerRequest) {
        validateRegistration(registerRequest); // Reuse validation

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getIdentifier())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles("ROLE_USER") // Assign standard user role
                .status("ACTIVE")   // Standard users are active immediately
                .build();
        User savedUser = userRepository.save(user);

        // Log them in immediately
        String token = jwtService.generateToken(savedUser);
        List<String> rolesList = getRolesAsList(savedUser);
        return new AuthResponse("User registered successfully!", savedUser.getUsername(), token, rolesList);
    }
    @Transactional
    public AuthResponse registerCookInitiate(RegisterRequest registerRequest) {
        validateRegistration(registerRequest); // Reuse validation

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getIdentifier())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles("ROLE_USER") // Assign base role initially
                .status("PENDING_COOK_PROFILE") // Mark as needing profile completion
                .build();
        // Cook specific fields (cookname, phone etc) are NULL here
        User savedUser = userRepository.save(user);

        // Log them in immediately so they can complete the profile
        String token = jwtService.generateToken(savedUser);
        List<String> rolesList = getRolesAsList(savedUser);
        // Send back token so frontend can proceed to profile setup while authenticated
        return new AuthResponse("Cook registration initiated. Please complete profile.", savedUser.getUsername(), token, rolesList);
    }

    // --- Shared Validation Logic ---
    private void validateRegistration(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.getIdentifier())) {
            throw new IllegalArgumentException("Error: Email is already taken!");
        }
    }
    public AuthResponse loginUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getIdentifier(),
                        loginRequest.getPassword()
                )
        );
        var user = userRepository.findByEmail(loginRequest.getIdentifier())
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication for email: " + loginRequest.getIdentifier()));
        if (!user.isEnabled() && !"PENDING_COOK_PROFILE".equals(user.getStatus())) {
            throw new IllegalStateException("User account is disabled.");
        }

        String token = jwtService.generateToken(user);
        List<String> rolesList = getRolesAsList(user);
        return new AuthResponse(
                "Login successful!",
                user.getUsername(), // Include username in the response
                token,
                rolesList
        );

    }
    private List<String> getRolesAsList(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
    @Transactional
    public User completeCookProfile(String setupToken, CookProfileDTO profileDto) {
        // Find user by the setup token and check expiry
        User user = userRepository.findBySetupToken(setupToken) // ** Need to add findBySetupToken to UserRepository **
                .filter(u -> u.getSetupTokenExpiry() != null && u.getSetupTokenExpiry().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired setup token."));

        // Update profile fields
        user.setCookname(profileDto.getCookname()); // Use getCookname() if field is cookname
        user.setPhone(profileDto.getPhone());
        user.setAvailabilityStatus(profileDto.getAvailabilityStatus());
        user.setLatitude(profileDto.getLatitude());
        user.setLongitude(profileDto.getLongitude());
        if (profileDto.getExpertise() != null) {
            user.setExpertise(profileDto.getExpertise());
        }

        // Activate user, assign cook role, clear token
        user.setStatus("ACTIVE");
        // Add ROLE_COOK, ensuring not to duplicate if somehow already present
        if (!user.getRoles().contains("ROLE_COOK")) {
            user.setRoles(user.getRoles() + ",ROLE_COOK");
        }
        user.setSetupToken(null);
        user.setSetupTokenExpiry(null);

        return userRepository.save(user);
    }
}