package com.smarttask.user.dto;

import com.smarttask.common.enums.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    @Size(max = 20) String phone,
    String profilePicture,
    Role role
) {}
