package com.cookedapp.cooked_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationInitResponse {
    private String message;
    private String setupToken; // The temporary token for profile setup
}