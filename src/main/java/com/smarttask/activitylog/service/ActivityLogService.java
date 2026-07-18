package com.smarttask.activitylog.service;

import com.smarttask.activitylog.dto.ActivityLogResponse;
import com.smarttask.activitylog.entity.ActivityLog;
import com.smarttask.activitylog.repository.ActivityLogRepository;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.user.entity.User;
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
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Async
    public void log(String action, String entityType, String entityId,
                    User performedBy, String description) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .description(description)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save activity log: {}", e.getMessage());
        }
    }

    @Async
    public void log(String action, String entityType, String entityId,
                    User performedBy, String description, String ipAddress) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception e) {
            // Never let logging failure break a business operation
            log.error("Failed to save activity log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getAllLogs(Pageable pageable) {
        Page<ActivityLog> page = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getLogsByEntity(
            String entityType, String entityId, Pageable pageable) {
        Page<ActivityLog> page = activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getLogsByUser(String userId, Pageable pageable) {
        Page<ActivityLog> page = activityLogRepository
                .findByPerformedByIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    private ActivityLogResponse toResponse(ActivityLog entry) {
        return ActivityLogResponse.builder()
                .id(entry.getId())
                .action(entry.getAction())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .performedById(entry.getPerformedBy() != null ? entry.getPerformedBy().getId() : null)
                .performedByName(entry.getPerformedBy() != null ? entry.getPerformedBy().getFullName() : null)
                .description(entry.getDescription())
                .ipAddress(entry.getIpAddress())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
