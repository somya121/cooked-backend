package com.cookedapp.cooked_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingStatusUpdateRequestDTO {
    @NotBlank(message = "New status cannot be blank")
    private String newStatus;
}