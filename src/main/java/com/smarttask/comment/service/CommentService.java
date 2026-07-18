package com.smarttask.comment.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.comment.dto.CommentRequest;
import com.smarttask.comment.dto.CommentResponse;
import com.smarttask.comment.entity.Comment;
import com.smarttask.comment.repository.CommentRepository;
import com.smarttask.common.enums.NotificationType;
import com.smarttask.common.exception.AccessDeniedException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.notification.service.NotificationService;
import com.smarttask.task.entity.Task;
import com.smarttask.task.repository.TaskRepository;
import com.smarttask.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Transactional
    public CommentResponse addComment(String taskId, CommentRequest request, User currentUser) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Comment comment = Comment.builder()
                .task(task)
                .author(currentUser)
                .message(request.message())
                .build();
        commentRepository.save(comment);

        // Notify task reporter and assignee (excluding comment author)
        if (task.getReporter() != null && !task.getReporter().getId().equals(currentUser.getId())) {
            notificationService.createNotification(
                    task.getReporter().getId(),
                    "New comment on task: " + task.getTitle(),
                    currentUser.getFullName() + " commented: " + truncate(request.message(), 80),
                    NotificationType.COMMENT_ADDED, task.getId(), "Task");
        }
        if (task.getAssignedUser() != null
                && !task.getAssignedUser().getId().equals(currentUser.getId())
                && (task.getReporter() == null || !task.getAssignedUser().getId().equals(task.getReporter().getId()))) {
            notificationService.createNotification(
                    task.getAssignedUser().getId(),
                    "New comment on task: " + task.getTitle(),
                    currentUser.getFullName() + " commented: " + truncate(request.message(), 80),
                    NotificationType.COMMENT_ADDED, task.getId(), "Task");
        }

        activityLogService.log("COMMENT_ADDED", "Task", taskId,
                currentUser, "Added comment on task: " + task.getTitle());
        return toResponse(comment);
    }

    @Transactional
    public CommentResponse editComment(String commentId, CommentRequest request, User currentUser) {
        Comment comment = findActiveOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only edit your own comments.");
        }
        comment.setMessage(request.message());
        commentRepository.save(comment);
        activityLogService.log("COMMENT_EDITED", "Comment", commentId,
                currentUser, "Edited comment on task: " + comment.getTask().getId());
        return toResponse(comment);
    }

    @Transactional
    public void deleteComment(String commentId, User currentUser) {
        Comment comment = findActiveOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only delete your own comments.");
        }
        comment.softDelete();
        commentRepository.save(comment);
        activityLogService.log("COMMENT_DELETED", "Comment", commentId,
                currentUser, "Deleted comment");
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getTaskComments(String taskId, Pageable pageable) {
        Page<Comment> page = commentRepository.findByTaskIdAndDeletedAtIsNull(taskId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    private Comment findActiveOrThrow(String id) {
        return commentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .taskId(c.getTask().getId())
                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                .authorName(c.getAuthor() != null ? c.getAuthor().getFullName() : null)
                .message(c.getMessage())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
