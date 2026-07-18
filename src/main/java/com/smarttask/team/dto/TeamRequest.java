package com.smarttask.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRequest(
    @NotBlank(message = "Team name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    String description,

    @NotNull(message = "Organization ID is required")
    String organizationId,

    String teamLeadId
) {}
