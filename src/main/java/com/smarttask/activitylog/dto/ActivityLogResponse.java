package com.smarttask.activitylog.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ActivityLogResponse(
    String id,
    String action,
    String entityType,
    String entityId,
    String performedById,
    String performedByName,
    String description,
    String ipAddress,
    LocalDateTime createdAt
) {}
