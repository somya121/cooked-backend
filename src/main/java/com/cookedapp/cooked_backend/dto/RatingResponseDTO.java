package com.cookedapp.cooked_backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponseDTO {
    private Long id;
    private Long bookingId;
    private Long ratedByUserId;
    private String ratedByUsername;
    private Long ratedCookId;
    private String ratedCookUsername;
    private Integer ratingValue;
    private String comment;
    private LocalDateTime createdAt;
}