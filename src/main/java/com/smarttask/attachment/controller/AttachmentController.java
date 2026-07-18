package com.smarttask.attachment.controller;

import com.smarttask.attachment.dto.AttachmentResponse;
import com.smarttask.attachment.service.FileAttachmentService;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "File Attachments", description = "Upload, download, and manage task file attachments")
public class AttachmentController {

    private final FileAttachmentService fileAttachmentService;

    @PostMapping("/tasks/{taskId}/upload")
    @Operation(summary = "Upload a file to a task")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @PathVariable String taskId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        fileAttachmentService.upload(taskId, file, SecurityUtils.getCurrentUser()),
                        "File uploaded successfully."));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "List all attachments for a task")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getTaskAttachments(
            @PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.success(
                fileAttachmentService.getTaskAttachments(taskId)));
    }

    @GetMapping("/{attachmentId}/metadata")
    @Operation(summary = "Get file metadata")
    public ResponseEntity<ApiResponse<AttachmentResponse>> getMetadata(
            @PathVariable String attachmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                fileAttachmentService.getMetadata(attachmentId)));
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> download(@PathVariable String attachmentId) {
        AttachmentResponse meta = fileAttachmentService.getMetadata(attachmentId);
        Resource resource = fileAttachmentService.download(attachmentId, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.fileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.fileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete a file attachment")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String attachmentId) {
        fileAttachmentService.delete(attachmentId, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("File deleted successfully."));
    }
}
