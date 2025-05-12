package com.cookedapp.cooked_backend.service; // Or a dedicated 'security' or 'util' package

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // 1. Load Secret Key from properties (NEVER hardcode it!)
    @Value("${application.security.jwt.secret-key}") // Example property name
    private String secretKeyString;

    @Value("${application.security.jwt.expiration}") // e.g., 86400000 (24 hours in ms)
    private long jwtExpiration;

    // --- Token Generation ---

    /**
     * Generates a JWT for the given UserDetails.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails); // Generate with empty extra claims
    }

    /**
     * Generates a JWT with extra claims.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    // You might want a separate method for refresh tokens with longer expiration
    // public String generateRefreshToken(UserDetails userDetails) { ... }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims) // Add custom claims first
                .subject(userDetails.getUsername()) // Set the username as the 'subject'
                .issuedAt(new Date(System.currentTimeMillis())) // Standard claim: issued time
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Standard claim: expiration time
                .signWith(getSignInKey(), Jwts.SIG.HS256) // Sign with HS256 Algorithm and the key
                // Deprecated approach (<0.12.x): .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact(); // Build the string token
    }

    // --- Token Validation ---

    /**
     * Checks if the token is valid for the given UserDetails.
     * Verifies signature, expiration, and if the username matches.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // --- Claim Extraction ---

    /**
     * Extracts the username (subject) from the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the token and extracts all claims.
     * Handles signature verification implicitly during parsing.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // Provide the key for verification
                // Deprecated approach (<0.12.x): .setSigningKey(getSignInKey())
                .build()
                .parseSignedClaims(token) // Parse and validate signature
                .getPayload(); // Get the claims payload
        // Note: This will throw exceptions (like ExpiredJwtException, SignatureException)
        // if the token is invalid or expired. These should be handled upstream (e.g., in the filter).
    }

    // --- Key Handling ---

    /**
     * Decodes the Base64 encoded secret key string into a SecretKey object.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}