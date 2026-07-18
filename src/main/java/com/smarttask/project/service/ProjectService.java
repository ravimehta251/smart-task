package com.smarttask.project.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.ProjectStatus;
import com.smarttask.common.enums.TaskStatus;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.organization.entity.Organization;
import com.smarttask.organization.repository.OrganizationRepository;
import com.smarttask.project.dto.ProjectDashboardResponse;
import com.smarttask.project.dto.ProjectRequest;
import com.smarttask.project.dto.ProjectResponse;
import com.smarttask.project.entity.Project;
import com.smarttask.project.repository.ProjectRepository;
import com.smarttask.task.repository.TaskRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public ProjectResponse create(ProjectRequest request, User currentUser) {
        Organization org = organizationRepository.findByIdAndDeletedAtIsNull(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.organizationId()));

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .status(ProjectStatus.PLANNING)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .organization(org)
                .createdBy(currentUser)
                .build();
        project.getMembers().add(currentUser);
        projectRepository.save(project);

        activityLogService.log("PROJECT_CREATED", "Project", project.getId(),
                currentUser, "Created project: " + project.getName());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(String id, ProjectRequest request, User currentUser) {
        Project project = findActiveOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        projectRepository.save(project);

        activityLogService.log("PROJECT_UPDATED", "Project", id,
                currentUser, "Updated project: " + project.getName());
        return toResponse(project);
    }

    @Transactional
    public void delete(String id, User currentUser) {
        Project project = findActiveOrThrow(id);
        project.softDelete();
        projectRepository.save(project);
        activityLogService.log("PROJECT_DELETED", "Project", id,
                currentUser, "Deleted project: " + project.getName());
    }

    @Transactional
    public ProjectResponse archive(String id, User currentUser) {
        Project project = findActiveOrThrow(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
        activityLogService.log("PROJECT_ARCHIVED", "Project", id,
                currentUser, "Archived project: " + project.getName());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse changeStatus(String id, ProjectStatus status, User currentUser) {
        Project project = findActiveOrThrow(id);
        project.setStatus(status);
        projectRepository.save(project);
        activityLogService.log("PROJECT_STATUS_CHANGED", "Project", id,
                currentUser, "Status changed to: " + status);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(String id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> getByOrganization(String orgId, String search,
                                                              ProjectStatus status, Pageable pageable) {
        Page<Project> page = projectRepository.searchProjects(orgId, search, status, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> getMyProjects(String userId, Pageable pageable) {
        Page<Project> page = projectRepository.findProjectsForUser(userId, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public ProjectResponse addMember(String projectId, String userId, User currentUser) {
        Project project = findActiveOrThrow(projectId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (project.getMembers().stream().anyMatch(m -> m.getId().equals(userId))) {
            throw new BusinessException("User is already a member of this project.");
        }
        project.getMembers().add(user);
        projectRepository.save(project);
        activityLogService.log("PROJECT_MEMBER_ADDED", "Project", projectId,
                currentUser, "Added member: " + user.getFullName());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse removeMember(String projectId, String userId, User currentUser) {
        Project project = findActiveOrThrow(projectId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        project.getMembers().removeIf(m -> m.getId().equals(userId));
        projectRepository.save(project);
        activityLogService.log("PROJECT_MEMBER_REMOVED", "Project", projectId,
                currentUser, "Removed member: " + user.getFullName());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectDashboardResponse getDashboard(String projectId) {
        Project project = findActiveOrThrow(projectId);
        long total = taskRepository.countByProjectIdAndDeletedAtIsNull(projectId);
        long todo = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.TODO);
        long inProgress = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.IN_PROGRESS);
        long inReview = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.IN_REVIEW);
        long done = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.DONE);
        double completion = total > 0 ? Math.round((done * 100.0 / total) * 10.0) / 10.0 : 0.0;

        return ProjectDashboardResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalTasks(total)
                .todoTasks(todo)
                .inProgressTasks(inProgress)
                .inReviewTasks(inReview)
                .doneTasks(done)
                .memberCount(project.getMembers().size())
                .completionPercentage(completion)
                .build();
    }

    private Project findActiveOrThrow(String id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .status(p.getStatus())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .organizationId(p.getOrganization().getId())
                .organizationName(p.getOrganization().getName())
                .createdById(p.getCreatedBy() != null ? p.getCreatedBy().getId() : null)
                .createdByName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null)
                .memberCount(p.getMembers().size())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
