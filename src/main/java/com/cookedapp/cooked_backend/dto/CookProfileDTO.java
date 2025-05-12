package com.cookedapp.cooked_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookProfileDTO {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String cookname;

    // Add validation as needed (e.g., @Pattern for phone)
    private String phone;

    private List<String> expertise; // List of expertise strings

    @NotBlank(message = "Availability status cannot be blank")
    private String availabilityStatus;

    // Location info - assuming frontend sends lat/lon directly for now
    // Alternatively, receive address fields and geocode in the backend
    private Double latitude;
    private Double longitude;

    // profileImageUrl will be handled separately if file upload is implemented,
    // or could be a URL provided by the client if they upload elsewhere.
    // For now, we might update it in the service if file upload is handled.
}