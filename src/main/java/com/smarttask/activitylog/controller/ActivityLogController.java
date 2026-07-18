package com.smarttask.activitylog.controller;

import com.smarttask.activitylog.dto.ActivityLogResponse;
import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Audit trail — every important action recorded")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "Get all activity logs (admin)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                activityLogService.getAllLogs(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get logs for a specific entity (task, project, etc.)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                activityLogService.getLogsByEntity(entityType, entityId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all actions performed by a specific user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                activityLogService.getLogsByUser(userId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}
