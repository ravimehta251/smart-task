package com.smarttask.organization.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.OrganizationStatus;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.organization.dto.OrganizationRequest;
import com.smarttask.organization.dto.OrganizationResponse;
import com.smarttask.organization.entity.Organization;
import com.smarttask.organization.repository.OrganizationRepository;
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
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public OrganizationResponse create(OrganizationRequest request, User currentUser) {
        if (organizationRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new BusinessException("An organization with this name already exists.");
        }
        Organization org = Organization.builder()
                .name(request.name())
                .description(request.description())
                .logo(request.logo())
                .website(request.website())
                .status(OrganizationStatus.ACTIVE)
                .build();
        organizationRepository.save(org);
        activityLogService.log("ORGANIZATION_CREATED", "Organization", org.getId(),
                currentUser, "Created organization: " + org.getName());
        return toResponse(org);
    }

    @Transactional
    public OrganizationResponse update(String id, OrganizationRequest request, User currentUser) {
        Organization org = findActiveOrThrow(id);
        if (!org.getName().equals(request.name())
                && organizationRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new BusinessException("An organization with this name already exists.");
        }
        org.setName(request.name());
        org.setDescription(request.description());
        org.setLogo(request.logo());
        org.setWebsite(request.website());
        organizationRepository.save(org);
        activityLogService.log("ORGANIZATION_UPDATED", "Organization", org.getId(),
                currentUser, "Updated organization: " + org.getName());
        return toResponse(org);
    }

    @Transactional
    public void delete(String id, User currentUser) {
        Organization org = findActiveOrThrow(id);
        org.softDelete();
        organizationRepository.save(org);
        activityLogService.log("ORGANIZATION_DELETED", "Organization", id,
                currentUser, "Deleted organization: " + org.getName());
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(String id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrganizationResponse> getAll(String search, Pageable pageable) {
        Page<Organization> page = organizationRepository.findAllActive(search, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public OrganizationResponse changeStatus(String id, OrganizationStatus status, User currentUser) {
        Organization org = findActiveOrThrow(id);
        org.setStatus(status);
        organizationRepository.save(org);
        activityLogService.log("ORGANIZATION_STATUS_CHANGED", "Organization", id,
                currentUser, "Status changed to: " + status);
        return toResponse(org);
    }

    private Organization findActiveOrThrow(String id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .logo(org.getLogo())
                .website(org.getWebsite())
                .status(org.getStatus())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
