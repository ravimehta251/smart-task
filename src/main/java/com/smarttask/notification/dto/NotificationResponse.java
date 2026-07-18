package com.smarttask.notification.dto;

import com.smarttask.common.enums.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
    String id,
    String title,
    String message,
    NotificationType type,
    boolean isRead,
    String referenceId,
    String referenceType,
    LocalDateTime createdAt
) {}
