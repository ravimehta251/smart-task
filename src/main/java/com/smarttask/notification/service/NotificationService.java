package com.smarttask.notification.service;

import com.smarttask.common.enums.NotificationType;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.notification.dto.NotificationResponse;
import com.smarttask.notification.entity.Notification;
import com.smarttask.notification.repository.NotificationRepository;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    public void createNotification(String recipientId, String title, String message,
                                    NotificationType type, String referenceId, String referenceType) {
        try {
            userRepository.findByIdAndDeletedAtIsNull(recipientId).ifPresent(recipient -> {
                Notification notification = Notification.builder()
                        .recipient(recipient)
                        .title(title)
                        .message(message)
                        .type(type)
                        .referenceId(referenceId)
                        .referenceType(referenceType)
                        .read(false)
                        .build();
                notificationRepository.save(notification);
            });
        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", recipientId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new com.smarttask.common.exception.AccessDeniedException(
                    "You can only mark your own notifications as read.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new com.smarttask.common.exception.AccessDeniedException(
                    "You can only delete your own notifications.");
        }
        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
