package com.smarttask.team.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record TeamResponse(
    String id,
    String name,
    String description,
    String organizationId,
    String organizationName,
    TeamMemberSummary teamLead,
    int memberCount,
    Set<TeamMemberSummary> members,
    LocalDateTime createdAt
) {}
