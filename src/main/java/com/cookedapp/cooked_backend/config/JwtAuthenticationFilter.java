package com.cookedapp.cooked_backend.config;

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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {


        final String authHeader = request.getHeader("Authorization");
        logger.info("JWT Filter: Received Authorization Header: {}"+authHeader);
        final String jwt;
        final String username;
        final String identifierFromToken;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }


        jwt = authHeader.substring(7);
        logger.info("JWT Filter: Extracted JWT string (first 30 chars): {}");
        try {
            identifierFromToken = jwtService.extractEmailFromToken(jwt);
            logger.info("JWT Filter: Extracted identifier (email) from token: [{}]"+identifierFromToken);
            if (identifierFromToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(identifierFromToken);
            }

            username = jwtService.extractEmailFromToken(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);


                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("JWT Filter: Token expired: {}"+ e.getMessage(),e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Token expired\"}");
            response.setContentType("application/json");
            return;
        } catch (SignatureException e) {
            logger.warn("JWT Filter: Invalid JWT signature: {}"+ e.getMessage(),e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Invalid JWT signature\"}");
            response.setContentType("application/json");
            return;
        } catch (MalformedJwtException e) {
            logger.warn("JWT Filter: Malformed JWT: {}",e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Malformed JWT\"}");
            response.setContentType("application/json");
            return;
        } catch (Exception e) {
            logger.error("JWT Filter: Could not set user authentication in security context", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Authentication processing error\"}");
            response.setContentType("application/json");
            return;
        }
    }
}