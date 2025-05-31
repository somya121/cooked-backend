package com.cookedapp.cooked_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdentifierCheckResponse {
    private boolean emailExists;
    private boolean userNameExists;
    private String identifier;
    private String username;
}