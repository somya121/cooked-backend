package com.cookedapp.cooked_backend.config; // Or filter package

import com.cookedapp.cooked_backend.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Make it a Spring bean
@RequiredArgsConstructor // Lombok for constructor injection
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Ensures filter runs only once per request

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Spring Security interface to load user data

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract Authorization Header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // If no token, pass to next filter
            return;
        }

        // 3. Extract JWT token (remove "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // 4. Extract username from token using JwtService
            username = jwtService.extractUsername(jwt);

            // 5. Check if username exists and user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load UserDetails from database (via UserDetailsService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 6. Validate token against UserDetails
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 7. Create Authentication token (trusted)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, // Principal
                            null, // Credentials (not needed for JWT)
                            userDetails.getAuthorities() // Authorities/Roles
                    );
                    // Set details from the request (e.g., IP address, session ID)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // 8. Update SecurityContextHolder - THIS IS HOW SPRING KNOWS THE USER IS AUTHENTICATED
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response); // Proceed to next filter

        } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
            logger.warn("JWT processing error: {}" + e.getMessage());

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Catch other potential errors during user loading or validation
            logger.error("Could not set user authentication in security context", e);
            filterChain.doFilter(request, response);
        }
    }
}