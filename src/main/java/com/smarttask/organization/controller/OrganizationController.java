package com.smarttask.organization.controller;

import com.smarttask.common.enums.OrganizationStatus;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import com.smarttask.organization.dto.OrganizationRequest;
import com.smarttask.organization.dto.OrganizationResponse;
import com.smarttask.organization.service.OrganizationService;
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
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Multi-tenant organization management")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @Operation(summary = "Create a new organization")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody OrganizationRequest request) {
        OrganizationResponse response = organizationService.create(request, SecurityUtils.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Organization created successfully."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable String id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                organizationService.update(id, request, SecurityUtils.getCurrentUser()),
                "Organization updated successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an organization (soft delete)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        organizationService.delete(id, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("Organization deleted successfully."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List all organizations with optional search")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizationResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                organizationService.getAll(search, PageRequest.of(page, size, sort))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change organization status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> changeStatus(
            @PathVariable String id, @RequestParam OrganizationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                organizationService.changeStatus(id, status, SecurityUtils.getCurrentUser()),
                "Status updated successfully."));
    }
}
