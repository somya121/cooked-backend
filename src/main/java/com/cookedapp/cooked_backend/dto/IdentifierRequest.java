package com.cookedapp.cooked_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdentifierRequest {

    @NotBlank(message = "Email identifier cannot be blank")
    @Email(message = "Identifier must be a valid email address")
    private String identifier;
    private String username;

}