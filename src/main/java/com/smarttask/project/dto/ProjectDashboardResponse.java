package com.smarttask.project.dto;

import lombok.Builder;

@Builder
public record ProjectDashboardResponse(
    String projectId,
    String projectName,
    long totalTasks,
    long todoTasks,
    long inProgressTasks,
    long inReviewTasks,
    long doneTasks,
    int memberCount,
    double completionPercentage
) {}
