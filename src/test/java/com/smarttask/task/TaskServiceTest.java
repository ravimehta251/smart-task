package com.smarttask.task;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.*;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.config.EmailService;
import com.smarttask.notification.service.NotificationService;
import com.smarttask.organization.entity.Organization;
import com.smarttask.project.entity.Project;
import com.smarttask.project.repository.ProjectRepository;
import com.smarttask.task.dto.TaskRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import com.smarttask.task.entity.Task;
import com.smarttask.task.repository.TaskRepository;
import com.smarttask.task.service.TaskService;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;
    @Mock EmailService emailService;

    @InjectMocks
    TaskService taskService;

    private User currentUser;
    private Project project;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder()
                .id("org-1").name("TechCorp").status(OrganizationStatus.ACTIVE).build();

        currentUser = User.builder()
                .id("user-1").firstName("Alice").lastName("Smith")
                .email("alice@example.com").role(Role.PROJECT_MANAGER)
                .status(UserStatus.ACTIVE).emailVerified(true).build();

        project = Project.builder()
                .id("proj-1").name("Backend API")
                .status(ProjectStatus.ACTIVE).organization(org)
                .createdBy(currentUser).build();

        existingTask = Task.builder()
                .id("task-1").title("Implement Auth")
                .description("JWT authentication").priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO).project(project).reporter(currentUser).build();
    }

    @Test
    @DisplayName("create — should save task and return response")
    void create_validRequest_returnsTaskResponse() {
        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest req = new TaskRequest(
                "Implement Auth", "JWT auth", TaskPriority.HIGH,
                "proj-1", null, null, null);

        TaskResponse response = taskService.create(req, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Implement Auth");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("create — should notify assignee when assigned at creation")
    void create_withAssignee_sendsNotification() {
        User assignee = User.builder()
                .id("user-2").firstName("Bob").lastName("Jones")
                .email("bob@example.com").role(Role.DEVELOPER)
                .status(UserStatus.ACTIVE).emailVerified(true).build();

        when(projectRepository.findByIdAndDeletedAtIsNull("proj-1"))
                .thenReturn(Optional.of(project));
        when(userRepository.findByIdAndDeletedAtIsNull("user-2"))
                .thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest req = new TaskRequest(
                "Fix Bug", "Fix null pointer", TaskPriority.CRITICAL,
                "proj-1", "user-2", null, null);

        taskService.create(req, currentUser);

        verify(notificationService).createNotification(
                eq("user-2"), anyString(), anyString(), eq(NotificationType.TASK_ASSIGNED),
                any(), eq("Task"));
        verify(emailService).sendTaskAssignedEmail(
                eq("bob@example.com"), eq("Bob Jones"), anyString(), anyString());
    }

    @Test
    @DisplayName("create — should throw when project not found")
    void create_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findByIdAndDeletedAtIsNull("bad-id"))
                .thenReturn(Optional.empty());

        TaskRequest req = new TaskRequest(
                "Task", null, TaskPriority.LOW, "bad-id", null, null, null);

        assertThatThrownBy(() -> taskService.create(req, currentUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update — should update fields and return response")
    void update_validRequest_updatesTask() {
        when(taskRepository.findByIdAndDeletedAtIsNull("task-1"))
                .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaskRequest req = new UpdateTaskRequest(
                "Implement JWT Auth", null, TaskPriority.CRITICAL,
                TaskStatus.IN_PROGRESS, null, null, null, null);

        TaskResponse response = taskService.update("task-1", req, currentUser);

        assertThat(response.title()).isEqualTo("Implement JWT Auth");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.CRITICAL);
    }

    @Test
    @DisplayName("delete — should soft-delete task")
    void delete_existingTask_softDeletesIt() {
        when(taskRepository.findByIdAndDeletedAtIsNull("task-1"))
                .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.delete("task-1", currentUser);

        assertThat(existingTask.getDeletedAt()).isNotNull();
        verify(taskRepository).save(existingTask);
    }

    @Test
    @DisplayName("changeStatus — should update status")
    void changeStatus_updatesTaskStatus() {
        when(taskRepository.findByIdAndDeletedAtIsNull("task-1"))
                .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.changeStatus("task-1", TaskStatus.DONE, currentUser);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }
}
