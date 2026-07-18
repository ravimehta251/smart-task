package com.smarttask.team.dto;

import com.smarttask.common.enums.Role;
import lombok.Builder;

@Builder
public record TeamMemberSummary(
    String id,
    String fullName,
    String email,
    Role role
) {}
