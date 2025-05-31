package com.cookedapp.cooked_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private String message;
    private Long bookingId;
    private String type;
    private LocalDateTime timestamp;
    private Long fromUserId;
    private Long toUserId; 
}