package com.cookedapp.cooked_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdentifierCheckResponse {
    private boolean emailExists; // True if the user exists, false otherwise
    private boolean userNameExists;
    private String identifier;  // Echo back the identifier checked
    private String username;
}