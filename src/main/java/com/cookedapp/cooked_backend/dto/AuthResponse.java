package com.cookedapp.cooked_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String message;
    private String username;
    private String token;
    private List<String> roles;
}