package com.smarttask.user.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.Role;
import com.smarttask.common.enums.UserStatus;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.user.dto.UpdateUserRequest;
import com.smarttask.user.dto.UserResponse;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> searchUsers(String orgId, String search,
                                                    Role role, UserStatus status,
                                                    Pageable pageable) {
        Page<User> page = userRepository.searchUsers(orgId, search, role, status, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(String id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional
    public UserResponse updateUser(String id, UpdateUserRequest request, User currentUser) {
        User user = findActiveOrThrow(id);

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName()  != null) user.setLastName(request.lastName());
        if (request.phone()     != null) user.setPhone(request.phone());
        if (request.profilePicture() != null) user.setProfilePicture(request.profilePicture());
        if (request.role()      != null) user.setRole(request.role());

        userRepository.save(user);
        activityLogService.log("USER_UPDATED", "User", id,
                currentUser, "Updated user profile: " + user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(String id, User currentUser) {
        User user = findActiveOrThrow(id);
        user.softDelete();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        activityLogService.log("USER_DELETED", "User", id,
                currentUser, "Deleted user: " + user.getEmail());
    }

    @Transactional
    public UserResponse changeRole(String id, Role role, User currentUser) {
        User user = findActiveOrThrow(id);
        user.setRole(role);
        userRepository.save(user);
        activityLogService.log("USER_ROLE_CHANGED", "User", id,
                currentUser, "Changed role to: " + role + " for user: " + user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public UserResponse deactivateUser(String id, User currentUser) {
        User user = findActiveOrThrow(id);
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("User is already inactive.");
        }
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        activityLogService.log("USER_DEACTIVATED", "User", id,
                currentUser, "Deactivated user: " + user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public UserResponse activateUser(String id, User currentUser) {
        User user = findActiveOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        activityLogService.log("USER_ACTIVATED", "User", id,
                currentUser, "Activated user: " + user.getEmail());
        return toResponse(user);
    }

    private User findActiveOrThrow(String id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .profilePicture(user.getProfilePicture())
                .emailVerified(user.isEmailVerified())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
