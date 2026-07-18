package com.smarttask.team.controller;

import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import com.smarttask.team.dto.TeamRequest;
import com.smarttask.team.dto.TeamResponse;
import com.smarttask.team.service.TeamService;
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
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management within organizations")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<TeamResponse>> create(
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        teamService.create(request, SecurityUtils.getCurrentUser()),
                        "Team created successfully."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable String id, @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                teamService.update(id, request, SecurityUtils.getCurrentUser()),
                "Team updated successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        teamService.delete(id, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("Team deleted successfully."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getById(id)));
    }

    @GetMapping("/organization/{orgId}")
    @Operation(summary = "List teams in an organization")
    public ResponseEntity<ApiResponse<PagedResponse<TeamResponse>>> getByOrganization(
            @PathVariable String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                teamService.getByOrganization(orgId, search,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Add a member to a team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<TeamResponse>> addMember(
            @PathVariable String teamId, @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                teamService.addMember(teamId, userId, SecurityUtils.getCurrentUser()),
                "Member added successfully."));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Remove a member from a team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMember(
            @PathVariable String teamId, @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                teamService.removeMember(teamId, userId, SecurityUtils.getCurrentUser()),
                "Member removed successfully."));
    }
}
