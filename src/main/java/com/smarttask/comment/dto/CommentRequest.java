package com.smarttask.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
    @NotBlank(message = "Comment message is required")
    String message
) {}
