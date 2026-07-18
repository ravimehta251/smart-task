package com.smarttask.comment.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CommentResponse(
    String id,
    String taskId,
    String authorId,
    String authorName,
    String message,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
