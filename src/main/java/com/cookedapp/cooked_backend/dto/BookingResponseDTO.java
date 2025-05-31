package com.cookedapp.cooked_backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponseDTO {
    private Long id;
    private Long customerId;
    private String customerUsername;
    private Long cookId;
    private String cookUsername;
    private String bookingDetails;
    private String customerName;
    private String customerAddress;
    private String mealPreference;
    private String bookingStatus;
    private LocalDateTime requestedDateTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double totalCharges;
    private LocalDateTime serviceCompletedAt;
    private LocalDateTime paymentCompletedAt;
    private boolean ratedByCurrentUser;
}