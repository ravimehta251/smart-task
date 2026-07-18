package com.smarttask.organization.dto;

import com.smarttask.common.enums.OrganizationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrganizationResponse(
    String id,
    String name,
    String description,
    String logo,
    String website,
    OrganizationStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
