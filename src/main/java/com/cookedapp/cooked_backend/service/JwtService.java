package com.cookedapp.cooked_backend.service;

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

    @Value("${application.security.jwt.secret-key}")
    private String secretKeyString;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }


    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        String subjectIdentifier = null;
        if (userDetails instanceof com.cookedapp.cooked_backend.entity.User) {
            subjectIdentifier = ((com.cookedapp.cooked_backend.entity.User) userDetails).getEmail();
            System.out.println("JwtService.buildToken - Email from User entity: " + subjectIdentifier);
            System.out.println("JwtService.buildToken - Username from User entity: " + ((com.cookedapp.cooked_backend.entity.User) userDetails).getUsername());
        } else {
            subjectIdentifier = userDetails.getUsername();
            System.out.println("JwtService.buildToken - Username from UserDetails (fallback): " + subjectIdentifier);
            if (subjectIdentifier == null || subjectIdentifier.trim().isEmpty()) {
                System.err.println("CRITICAL ERROR in JwtService.buildToken: subjectIdentifier (email) is null or empty for user: " + userDetails.getUsername());

            }
        }
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subjectIdentifier)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String emailFromToken = extractEmailFromToken(token);
        String emailFromUserDetails = null;
        if (userDetails instanceof com.cookedapp.cooked_backend.entity.User) {
            emailFromUserDetails = ((com.cookedapp.cooked_backend.entity.User) userDetails).getEmail();
        } else {

            return false;
        }

        return (emailFromToken.equals(emailFromUserDetails)) && !isTokenExpired(token);

    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }



    public String extractEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}