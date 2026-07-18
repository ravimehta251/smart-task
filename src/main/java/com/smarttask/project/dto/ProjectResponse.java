package com.smarttask.project.dto;

import com.smarttask.common.enums.ProjectStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ProjectResponse(
    String id,
    String name,
    String description,
    ProjectStatus status,
    LocalDate startDate,
    LocalDate endDate,
    String organizationId,
    String organizationName,
    String createdById,
    String createdByName,
    int memberCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
