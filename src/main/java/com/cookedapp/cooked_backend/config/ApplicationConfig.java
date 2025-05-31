package com.cookedapp.cooked_backend.config;

import com.cookedapp.cooked_backend.controller.AuthController;
import com.cookedapp.cooked_backend.entity.User;
import com.cookedapp.cooked_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository; // Inject repository

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            logger.info("UserDetailsService: Attempting to load user by email: [{}]", email); // Use logger
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                logger.warn("UserDetailsService: User NOT FOUND with email: [{}]", email);
                throw new UsernameNotFoundException("User not found with email: " + email);
            }
            User user = userOptional.get();
            logger.info("UserDetailsService: User FOUND: Username [{}], Email [{}], Status [{}], Roles [{}]",
                    user.getUsername(), user.getEmail(), user.getStatus(), user.getRoles());
            return user;
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService(userRepository)); // Calls the bean method above
        authProvider.setPasswordEncoder(passwordEncoder());     // Calls the bean method above
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Add an interceptor to set the User-Agent header for Nominatim
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "YourAppName/1.0 (your-app-contact-info-or-url)");
            return execution.execute(request, body);
        }));
        return restTemplate;
    }

}