package com.smarttask.user.dto;

import com.smarttask.common.enums.Role;
import com.smarttask.common.enums.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
    String id,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    Role role,
    UserStatus status,
    String profilePicture,
    boolean emailVerified,
    String organizationId,
    String organizationName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
