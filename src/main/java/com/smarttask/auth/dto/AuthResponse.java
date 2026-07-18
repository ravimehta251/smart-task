package com.smarttask.auth.dto;

import com.smarttask.common.enums.Role;
import lombok.Builder;

@Builder
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String userId,
    String email,
    String fullName,
    Role role
) {}
