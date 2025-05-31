package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.MessageResponse;
import com.cookedapp.cooked_backend.service.GeocodingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600) // Or use your global CORS config
public class LocationController {

    private final GeocodingService geocodingService;
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @PostConstruct // This method runs after the bean is constructed and dependencies are injected
    public void init() {
        logger.info("<<<<< LocationController has been INITIALIZED and MAPPED to /api/location >>>>>");
        if (geocodingService == null) {
            logger.error("<<<<< GeocodingService is NULL in LocationController after construction! >>>>>");
        } else {
            logger.info("<<<<< GeocodingService is successfully injected into LocationController. >>>>>");
        }
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<?> getPlaceNameFromCoordinates(
            @RequestParam double lat,
            @RequestParam double lon) {
        logger.info("<<<<< LOCATION CONTROLLER /reverse-geocode HIT with lat: {}, lon: {} >>>>>", lat, lon);
        String placeName = null;
        try {
             placeName = geocodingService.getPlaceName(lat, lon);
            Map<String, String> response = new HashMap<>();
            if (placeName != null && !placeName.isEmpty()) {
                response.put("displayName", placeName);
                return ResponseEntity.ok(response);
            } else {
                response.put("displayName", "Area name not found"); // Or just an empty string
                logger.warn("Reverse geocode for lat={}, lon={} resulted in no place name or service error.", lat, lon);
                return ResponseEntity.ok(response); // This also returns JSON            }
            }
        }catch (Exception e) {
                logger.error("Unexpected error in /reverse-geocode for lat={}, lon={}: {}", lat, lon, e.getMessage(), e);
                // THIS IS A POTENTIAL CULPRIT if this block is reached and returns HTML by default
                // Ensure it returns JSON.
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch place name due to an internal error.");
                errorResponse.put("message", e.getMessage()); // Be cautious about exposing raw error messages
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);        }
    }
}