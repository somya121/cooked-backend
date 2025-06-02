package com.cookedapp.cooked_backend.config;

import com.cookedapp.cooked_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Arrays;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl, "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/images/**", configuration);
        source.registerCorsConfiguration("/ws-cookapp/**", configuration);
        System.out.println("CORS Configuration Initialized with Allowed Origins: " + configuration.getAllowedOrigins());
        return source;
    }



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/check-identifier",
                                "/api/auth/register/user",
                                "/api/auth/register/cook",
                                "/api/auth/login",
                                "/ws-cookapp/**",
                                "/images/**",
                                "/api/location/reverse-geocode"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/bookings/user/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/profile/picture").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/nearby").authenticated()
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/ratings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/bookings/{bookingId}/complete-service").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/bookings/{bookingId}/receive-payment").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/bookings/{bookingId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/cook/{cookId}").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}