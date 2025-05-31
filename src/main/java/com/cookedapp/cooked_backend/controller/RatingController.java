package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.MessageResponse;
import com.cookedapp.cooked_backend.dto.RatingRequestDTO;
import com.cookedapp.cooked_backend.dto.RatingResponseDTO;
import com.cookedapp.cooked_backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RatingController {
    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);
    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<?> submitRating(
            @Valid @RequestBody RatingRequestDTO ratingRequest,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            RatingResponseDTO rating = ratingService.submitRating(ratingRequest, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(rating);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to submit rating: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting rating: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not submit rating."));
        }
    }

    @GetMapping("/cook/{cookId}")
    public ResponseEntity<?> getRatingsForCook(@PathVariable Long cookId) {
        try {
            List<RatingResponseDTO> ratings = ratingService.getRatingsForCook(cookId);
            return ResponseEntity.ok(ratings);
        } catch (UsernameNotFoundException e) {
            logger.warn("Failed to get ratings for cook {}: {}", cookId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching ratings for cook {}: ", cookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not fetch ratings."));
        }
    }
}
