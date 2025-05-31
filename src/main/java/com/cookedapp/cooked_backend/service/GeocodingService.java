package com.cookedapp.cooked_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeocodingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getPlaceName(double latitude, double longitude) {
        String baseUrl = "https://nominatim.openstreetmap.org/reverse";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("format", "jsonv2")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("zoom", 16)
                .queryParam("addressdetails", 1);

        String url = uriBuilder.toUriString();
        logger.info("Requesting reverse geocoding from Nominatim URL: {}", url);

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            logger.info("RAW response from Nominatim: {}", responseBody);

            if (responseBody != null && !responseBody.trim().isEmpty()) {
                if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                    JsonNode root = objectMapper.readTree(responseBody);

                    String rootName = root.has("name") ? root.get("name").asText(null) : null;

                    if (root.has("address")) {
                        JsonNode address = root.get("address");
                        String road = address.has("road") ? address.get("road").asText(null) : null;
                        String neighbourhood = address.has("neighbourhood") ? address.get("neighbourhood").asText(null) : null;
                        String quarter = address.has("quarter") ? address.get("quarter").asText(null) : null;
                        String suburb = address.has("suburb") ? address.get("suburb").asText(null) : null;
                        String village = address.has("village") ? address.get("village").asText(null) : null;
                        String town = address.has("town") ? address.get("town").asText(null) : null;
                        String cityDistrict = address.has("city_district") ? address.get("city_district").asText(null) : null;
                        String city = address.has("city") ? address.get("city").asText(null) : null;
                        String county = address.has("county") ? address.get("county").asText(null) : null;
                        String state = address.has("state") ? address.get("state").asText(null) : null;

                        String primaryPlace = null;

                        if (isValidPlaceName(rootName) && !isBroaderThan(rootName, city, town, village, suburb, quarter, neighbourhood, cityDistrict)) {
                            primaryPlace = rootName;
                        } else if (isValidPlaceName(neighbourhood)) {
                            primaryPlace = neighbourhood;
                        } else if (isValidPlaceName(quarter)) {
                            primaryPlace = quarter;
                        } else if (isValidPlaceName(suburb)) {
                            primaryPlace = suburb;
                        } else if (isValidPlaceName(village)) {
                            primaryPlace = village;
                        } else if (isValidPlaceName(town)) {
                            primaryPlace = town;
                        } else if (isValidPlaceName(cityDistrict)) {
                            primaryPlace = cityDistrict;
                        } else if (isValidPlaceName(city)) {
                            primaryPlace = city;
                        } else if (isValidPlaceName(county)) {
                            primaryPlace = county;
                        }


                        if (primaryPlace != null) {

                            if (state != null && !state.equalsIgnoreCase(primaryPlace) &&
                                    (city == null || !city.equalsIgnoreCase(state)) &&
                                    (town == null || !town.equalsIgnoreCase(state)) &&
                                    (village == null || !village.equalsIgnoreCase(state)) &&
                                    (suburb == null || !suburb.equalsIgnoreCase(state)) &&
                                    (quarter == null || !quarter.equalsIgnoreCase(state)) &&
                                    (neighbourhood == null || !neighbourhood.equalsIgnoreCase(state)) &&
                                    (cityDistrict == null || !cityDistrict.equalsIgnoreCase(state)) &&
                                    (county == null || !county.equalsIgnoreCase(state))
                            ) {
                                logger.info("Geocoding result: Primary='{}', State='{}'", primaryPlace, state);
                                return primaryPlace + ", " + state;
                            }
                            logger.info("Geocoding result: Primary='{}'", primaryPlace);
                            return primaryPlace;
                        }
                    }
                    if (root.has("display_name")) {
                        String displayName = root.get("display_name").asText();
                        String firstComponent = displayName.split(",")[0].trim();
                        logger.warn("Falling back to first component of display_name ('{}') for URL [{}]. Full display_name: {}", firstComponent, url, displayName);
                        return firstComponent;
                    }
                } else {
                    logger.warn("Nominatim response for URL [{}] does not appear to be JSON: {}", url, responseBody);
                }
            } else {
                logger.warn("Received null or empty response from Nominatim for URL: {}", url);
            }
        } catch (Exception e) {
            logger.error("Error during Nominatim call or JSON parsing for lat={}, lon={}: {}", latitude, longitude, e.getMessage(), e);
        }
        logger.warn("Geocoding failed to produce a place name for lat={}, lon={}", latitude, longitude);
        return null;
    }


    private boolean isValidPlaceName(String name) {
        return name != null && !name.trim().isEmpty() && !name.matches("\\d+");
    }

    private boolean isBroaderThan(String rootName, String... specificNames) {
        if (rootName == null) return false;
        for (String specificName : specificNames) {
            if (specificName != null && rootName.equalsIgnoreCase(specificName)) {
                return true;
            }
        }
        return false;
    }
}