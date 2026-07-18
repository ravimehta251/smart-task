package com.smarttask.project.controller;

import com.smarttask.common.enums.ProjectStatus;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import com.smarttask.project.dto.ProjectDashboardResponse;
import com.smarttask.project.dto.ProjectRequest;
import com.smarttask.project.dto.ProjectResponse;
import com.smarttask.project.service.ProjectService;
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
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project lifecycle management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        projectService.create(request, SecurityUtils.getCurrentUser()),
                        "Project created successfully."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable String id, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.update(id, request, SecurityUtils.getCurrentUser()),
                "Project updated successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (soft delete)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        projectService.delete(id, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("Project deleted successfully."));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a project")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> archive(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.archive(id, SecurityUtils.getCurrentUser()),
                "Project archived."));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change project status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> changeStatus(
            @PathVariable String id, @RequestParam ProjectStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.changeStatus(id, status, SecurityUtils.getCurrentUser()),
                "Status updated."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getById(id)));
    }

    @GetMapping("/organization/{orgId}")
    @Operation(summary = "List projects for an organization")
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getByOrganization(
            @PathVariable String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                projectService.getByOrganization(orgId, search, status, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/my")
    @Operation(summary = "Get projects I am a member of")
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getMyProjects(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.getMyProjects(SecurityUtils.getCurrentUserId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Add a member to a project")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> addMember(
            @PathVariable String projectId, @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.addMember(projectId, userId, SecurityUtils.getCurrentUser()),
                "Member added to project."));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Remove a member from a project")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> removeMember(
            @PathVariable String projectId, @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.removeMember(projectId, userId, SecurityUtils.getCurrentUser()),
                "Member removed from project."));
    }

    @GetMapping("/{id}/dashboard")
    @Operation(summary = "Get project dashboard with task statistics")
    public ResponseEntity<ApiResponse<ProjectDashboardResponse>> getDashboard(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getDashboard(id)));
    }
}
