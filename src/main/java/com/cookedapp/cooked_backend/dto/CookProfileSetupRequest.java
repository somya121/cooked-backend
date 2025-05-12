package com.cookedapp.cooked_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CookProfileSetupRequest {

    @NotBlank(message = "Setup token cannot be blank")
    private String setupToken;

    @Valid // Validate the nested DTO
    private CookProfileDTO profileData;
}