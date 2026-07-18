package com.smarttask.task.dto;

import com.smarttask.common.enums.TaskPriority;
import com.smarttask.common.enums.TaskStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record TaskResponse(
    String id,
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate,
    BigDecimal estimatedHours,
    BigDecimal actualHours,
    String projectId,
    String projectName,
    String assignedUserId,
    String assignedUserName,
    String reporterId,
    String reporterName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
