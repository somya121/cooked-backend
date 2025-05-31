package com.cookedapp.cooked_backend.service;

import com.cookedapp.cooked_backend.dto.NotificationPayload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(Long userId, NotificationPayload payload) {
        String destination = "/topic/user/" + userId + "/notifications";
        payload.setTimestamp(LocalDateTime.now());
        payload.setToUserId(userId);
        logger.info("Attempting to send notification to destination {} for userId {} with payload: {}", destination, userId, payload);
        messagingTemplate.convertAndSend(destination, payload);
        logger.info("Notification supposedly sent to {}.", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void sendNotificationToCook(Long cookId, NotificationPayload payload) {
        String destination = "/topic/cook/" + cookId + "/notifications";
        payload.setTimestamp(LocalDateTime.now());
        payload.setToUserId(cookId);
        logger.info("Sending notification to {}: {}", destination, payload.getMessage());
        messagingTemplate.convertAndSend(destination, payload);
    }
}