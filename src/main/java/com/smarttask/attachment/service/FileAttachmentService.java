package com.smarttask.attachment.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.attachment.dto.AttachmentResponse;
import com.smarttask.attachment.entity.FileAttachment;
import com.smarttask.attachment.repository.FileAttachmentRepository;
import com.smarttask.common.exception.AccessDeniedException;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.task.entity.Task;
import com.smarttask.task.repository.TaskRepository;
import com.smarttask.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;

    @Value("${application.file.upload-dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip", "application/x-zip-compressed",
            "text/plain"
    );

    @Transactional
    public AttachmentResponse upload(String taskId, MultipartFile file, User currentUser) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("File type not allowed: " + contentType);
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path targetDir = Paths.get(uploadDir, taskId);
        Path targetPath = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage());
            throw new BusinessException("Failed to store file. Please try again.");
        }

        FileAttachment attachment = FileAttachment.builder()
                .task(task)
                .fileName(originalName)
                .fileType(contentType)
                .fileSize(file.getSize())
                .filePath(targetPath.toString())
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();
        fileAttachmentRepository.save(attachment);

        activityLogService.log("FILE_UPLOADED", "Task", taskId,
                currentUser, "Uploaded file: " + originalName);
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTaskAttachments(String taskId) {
        return fileAttachmentRepository.findByTaskIdAndDeletedAtIsNull(taskId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttachmentResponse getMetadata(String attachmentId) {
        return toResponse(findActiveOrThrow(attachmentId));
    }

    public Resource download(String attachmentId, User currentUser) {
        FileAttachment attachment = findActiveOrThrow(attachmentId);
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("File not found or not readable.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException("Could not resolve file path.");
        }
    }

    @Transactional
    public void delete(String attachmentId, User currentUser) {
        FileAttachment attachment = findActiveOrThrow(attachmentId);

        boolean isOwner = attachment.getUploadedBy() != null
                && attachment.getUploadedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().contains("ADMIN")
                || currentUser.getRole().name().contains("PROJECT_MANAGER");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You don't have permission to delete this file.");
        }

        try {
            Files.deleteIfExists(Paths.get(attachment.getFilePath()));
        } catch (IOException e) {
            log.warn("Could not delete physical file: {}", e.getMessage());
        }

        attachment.setDeletedAt(LocalDateTime.now());
        fileAttachmentRepository.save(attachment);
        activityLogService.log("FILE_DELETED", "Task", attachment.getTask().getId(),
                currentUser, "Deleted file: " + attachment.getFileName());
    }

    private FileAttachment findActiveOrThrow(String id) {
        return fileAttachmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private AttachmentResponse toResponse(FileAttachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .taskId(a.getTask().getId())
                .fileName(a.getFileName())
                .fileType(a.getFileType())
                .fileSize(a.getFileSize())
                .uploadedById(a.getUploadedBy() != null ? a.getUploadedBy().getId() : null)
                .uploadedByName(a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : null)
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
