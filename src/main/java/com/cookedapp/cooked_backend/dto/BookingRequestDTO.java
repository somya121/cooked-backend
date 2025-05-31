package com.cookedapp.cooked_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequestDTO {
    @NotNull(message = "Cook ID cannot be null")
    @Min(value = 1, message = "Invalid Cook ID")
    private Long cookId;

    @NotBlank(message = "Booking details cannot be blank")
    private String customerName;

    @NotBlank(message = "Booking address cannot be blank")
    private String customerAddress;

    private String mealPreference;

    private LocalDateTime requestedDateTime;
}