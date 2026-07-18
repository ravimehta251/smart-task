package com.smarttask.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    String description,

    @NotNull(message = "Organization ID is required")
    String organizationId,

    LocalDate startDate,
    LocalDate endDate
) {}
