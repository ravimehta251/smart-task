package com.smarttask.project;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.*;
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
import com.smarttask.project.service.ProjectService;
import com.smarttask.task.repository.TaskRepository;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;
    @Mock ActivityLogService activityLogService;

    @InjectMocks
    ProjectService projectService;

    private User currentUser;
    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id("org-1").name("TechCorp")
                .status(OrganizationStatus.ACTIVE).build();

        currentUser = User.builder()
                .id("user-1").firstName("Alice").lastName("Smith")
                .email("alice@example.com").role(Role.PROJECT_MANAGER)
                .status(UserStatus.ACTIVE).emailVerified(true).build();

        project = Project.builder()
                .id("proj-1").name("SmartTask Backend")
                .description("Main backend project")
                .status(ProjectStatus.PLANNING)
                .organization(organization)
                .createdBy(currentUser)
                .build();
        project.getMembers().add(currentUser);
    }

    @Test
    @DisplayName("create — should build project with PLANNING status and add creator as member")
    void create_validRequest_createsProject() {
        when(organizationRepository.findByIdAndDeletedAtIsNull("org-1"))
                .thenReturn(Optional.of(organization));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRequest req = new ProjectRequest(
                "SmartTask Backend", "Main backend",
                "org-1", LocalDate.now(), LocalDate.now().plusMonths(3));

        ProjectResponse resp = projectService.create(req, currentUser);

        assertThat(resp.name()).isEqualTo("SmartTask Backend");
        assertThat(resp.status()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(resp.organizationId()).isEqualTo("org-1");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("create — should throw when organization not found")
    void create_orgNotFound_throwsResourceNotFoundException() {
        when(organizationRepository.findByIdAndDeletedAtIsNull("bad-org"))
                .thenReturn(Optional.empty());

        ProjectRequest req = new ProjectRequest(
                "Project X", null, "bad-org", null, null);

        assertThatThrownBy(() -> projectService.create(req, currentUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("archive — should set status to ARCHIVED")
    void archive_existingProject_setsArchivedStatus() {
        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse resp = projectService.archive("proj-1", currentUser);

        assertThat(resp.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    @DisplayName("delete — should soft-delete project")
    void delete_existingProject_setsDeletedAt() {
        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        projectService.delete("proj-1", currentUser);

        assertThat(project.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("addMember — should throw if user already a member")
    void addMember_alreadyMember_throwsBusinessException() {
        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(userRepository.findByIdAndDeletedAtIsNull("user-1"))
                .thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> projectService.addMember("proj-1", "user-1", currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    @DisplayName("getDashboard — should return correct task counts and completion percentage")
    void getDashboard_returnsCorrectStats() {
        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(taskRepository.countByProjectIdAndDeletedAtIsNull("proj-1")).thenReturn(10L);
        when(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull("proj-1", TaskStatus.TODO))
                .thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull("proj-1", TaskStatus.IN_PROGRESS))
                .thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull("proj-1", TaskStatus.IN_REVIEW))
                .thenReturn(1L);
        when(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull("proj-1", TaskStatus.DONE))
                .thenReturn(4L);

        ProjectDashboardResponse dash = projectService.getDashboard("proj-1");

        assertThat(dash.totalTasks()).isEqualTo(10L);
        assertThat(dash.doneTasks()).isEqualTo(4L);
        assertThat(dash.completionPercentage()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("getByOrganization — should return paged results")
    void getByOrganization_returnsPagedResponse() {
        var pageRequest = PageRequest.of(0, 10);
        when(projectRepository.searchProjects("org-1", null, null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(project)));

        PagedResponse<ProjectResponse> result =
                projectService.getByOrganization("org-1", null, null, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("SmartTask Backend");
    }
}
