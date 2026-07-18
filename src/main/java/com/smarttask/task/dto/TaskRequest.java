package com.smarttask.task.dto;

import com.smarttask.common.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank(message = "Task title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    String title,

    String description,

    @NotNull(message = "Priority is required")
    TaskPriority priority,

    @NotNull(message = "Project ID is required")
    String projectId,

    String assignedUserId,
    LocalDate dueDate,
    BigDecimal estimatedHours
) {}
