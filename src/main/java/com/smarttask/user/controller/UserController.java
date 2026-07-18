package com.smarttask.user.controller;

import com.smarttask.common.enums.Role;
import com.smarttask.common.enums.UserStatus;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import com.smarttask.user.dto.UpdateUserRequest;
import com.smarttask.user.dto.UserResponse;
import com.smarttask.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User / employee management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Search and list users with pagination, filtering, sorting")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> search(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                userService.searchUsers(orgId, search, role, status, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getById(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUser(id, request, SecurityUtils.getCurrentUser()),
                "User updated successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.deleteUser(id, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("User deleted successfully."));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable String id, @RequestParam Role role) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.changeRole(id, role, SecurityUtils.getCurrentUser()),
                "Role updated successfully."));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.deactivateUser(id, SecurityUtils.getCurrentUser()),
                "User deactivated."));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.activateUser(id, SecurityUtils.getCurrentUser()),
                "User activated."));
    }
}
