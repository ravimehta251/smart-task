package com.smarttask.attachment.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AttachmentResponse(
    String id,
    String taskId,
    String fileName,
    String fileType,
    Long fileSize,
    String uploadedById,
    String uploadedByName,
    LocalDateTime uploadedAt
) {}
