package com.smarttask.task.dto;

import com.smarttask.common.enums.TaskPriority;
import com.smarttask.common.enums.TaskStatus;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTaskRequest(
    @Size(max = 300) String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate,
    BigDecimal estimatedHours,
    BigDecimal actualHours,
    String assignedUserId
) {}
