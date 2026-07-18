package com.smarttask.task.controller;

import com.smarttask.common.enums.TaskPriority;
import com.smarttask.common.enums.TaskStatus;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import com.smarttask.task.dto.TaskRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import com.smarttask.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Core task management — create, assign, track, search")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER','TEAM_LEAD','DEVELOPER')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        taskService.create(request, SecurityUtils.getCurrentUser()),
                        "Task created successfully."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER','TEAM_LEAD','DEVELOPER','TESTER')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable String id, @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.update(id, request, SecurityUtils.getCurrentUser()),
                "Task updated successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task (soft delete)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        taskService.delete(id, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("Task deleted successfully."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getById(id)));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Search tasks in a project with filters, pagination, sorting")
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponse>>> searchByProject(
            @PathVariable String projectId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                taskService.searchTasks(projectId, search, status, priority,
                        PageRequest.of(page, size, sort))));
    }

    @GetMapping("/my")
    @Operation(summary = "Get tasks assigned to me")
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponse>>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.getMyTasks(SecurityUtils.getCurrentUserId(), status,
                        PageRequest.of(page, size, Sort.by("dueDate").ascending()))));
    }

    @PatchMapping("/{taskId}/assign/{userId}")
    @Operation(summary = "Assign a task to a user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER','TEAM_LEAD')")
    public ResponseEntity<ApiResponse<TaskResponse>> assign(
            @PathVariable String taskId, @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.assignTask(taskId, userId, SecurityUtils.getCurrentUser()),
                "Task assigned successfully."));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Change task status (move task)")
    public ResponseEntity<ApiResponse<TaskResponse>> changeStatus(
            @PathVariable String taskId, @RequestParam TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.changeStatus(taskId, status, SecurityUtils.getCurrentUser()),
                "Task status updated."));
    }
}
