package com.smarttask.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
    @NotBlank(message = "Organization name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    String description,

    @Size(max = 500, message = "Logo URL too long")
    String logo,

    @Size(max = 300, message = "Website URL too long")
    String website
) {}
