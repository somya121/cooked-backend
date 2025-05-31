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

    private Long id;
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String cookname;

    private String phone;

    private List<String> expertise;

    @NotBlank(message = "Availability status cannot be blank")
    private String availabilityStatus;
    private String profilePicture;
    private Double latitude;
    private Double longitude;
    private Double chargesPerMeal;
    private Double averageRating;
    private Integer numberOfRatings;
    private String placeName;

}