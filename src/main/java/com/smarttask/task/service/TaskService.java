package com.smarttask.task.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.enums.NotificationType;
import com.smarttask.common.enums.TaskStatus;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.enums.TaskPriority;
import com.smarttask.config.EmailService;
import com.smarttask.notification.service.NotificationService;
import com.smarttask.project.entity.Project;
import com.smarttask.project.repository.ProjectRepository;
import com.smarttask.task.dto.TaskRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import com.smarttask.task.entity.Task;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;

    @Transactional
    public TaskResponse create(TaskRequest request, User currentUser) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.projectId()));

        User assignedUser = null;
        if (request.assignedUserId() != null) {
            assignedUser = userRepository.findByIdAndDeletedAtIsNull(request.assignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedUserId()));
        }

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .status(TaskStatus.TODO)
                .dueDate(request.dueDate())
                .estimatedHours(request.estimatedHours())
                .project(project)
                .assignedUser(assignedUser)
                .reporter(currentUser)
                .build();

        taskRepository.save(task);

        if (assignedUser != null) {
            notificationService.createNotification(
                    assignedUser.getId(),
                    "New task assigned: " + task.getTitle(),
                    "You have been assigned a new task in project: " + project.getName(),
                    NotificationType.TASK_ASSIGNED, task.getId(), "Task");
            emailService.sendTaskAssignedEmail(
                    assignedUser.getEmail(), assignedUser.getFullName(),
                    task.getTitle(), project.getName());
        }

        activityLogService.log("TASK_CREATED", "Task", task.getId(),
                currentUser, "Created task: " + task.getTitle());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(String id, UpdateTaskRequest request, User currentUser) {
        Task task = findActiveOrThrow(id);
        String oldAssignedId = task.getAssignedUser() != null ? task.getAssignedUser().getId() : null;

        if (request.title()       != null) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.priority()    != null) task.setPriority(request.priority());
        if (request.status()      != null) task.setStatus(request.status());
        if (request.dueDate()     != null) task.setDueDate(request.dueDate());
        if (request.estimatedHours() != null) task.setEstimatedHours(request.estimatedHours());
        if (request.actualHours() != null) task.setActualHours(request.actualHours());

        if (request.assignedUserId() != null) {
            User newAssignee = userRepository.findByIdAndDeletedAtIsNull(request.assignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedUserId()));
            task.setAssignedUser(newAssignee);

            if (!request.assignedUserId().equals(oldAssignedId)) {
                notificationService.createNotification(
                        newAssignee.getId(),
                        "Task assigned: " + task.getTitle(),
                        "You have been assigned a task in project: " + task.getProject().getName(),
                        NotificationType.TASK_ASSIGNED, task.getId(), "Task");
            }
        }

        taskRepository.save(task);

        if (task.getStatus() == TaskStatus.DONE) {
            notificationService.createNotification(
                    task.getReporter() != null ? task.getReporter().getId() : currentUser.getId(),
                    "Task completed: " + task.getTitle(),
                    "The task has been marked as done.",
                    NotificationType.TASK_COMPLETED, task.getId(), "Task");
        }

        activityLogService.log("TASK_UPDATED", "Task", id,
                currentUser, "Updated task: " + task.getTitle());
        return toResponse(task);
    }

    @Transactional
    public void delete(String id, User currentUser) {
        Task task = findActiveOrThrow(id);
        task.softDelete();
        taskRepository.save(task);
        activityLogService.log("TASK_DELETED", "Task", id,
                currentUser, "Deleted task: " + task.getTitle());
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(String id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> searchTasks(String projectId, String search,
                                                    TaskStatus status, TaskPriority priority,
                                                    Pageable pageable) {
        Page<Task> page = taskRepository.searchTasks(projectId, search, status, priority, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> getMyTasks(String userId, TaskStatus status, Pageable pageable) {
        Page<Task> page = taskRepository.findTasksAssignedToUser(userId, status, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public TaskResponse assignTask(String taskId, String userId, User currentUser) {
        Task task = findActiveOrThrow(taskId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        task.setAssignedUser(user);
        taskRepository.save(task);

        notificationService.createNotification(
                user.getId(),
                "Task assigned: " + task.getTitle(),
                "You have been assigned a task in project: " + task.getProject().getName(),
                NotificationType.TASK_ASSIGNED, task.getId(), "Task");

        activityLogService.log("TASK_ASSIGNED", "Task", taskId,
                currentUser, "Assigned task to: " + user.getFullName());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse changeStatus(String taskId, TaskStatus status, User currentUser) {
        Task task = findActiveOrThrow(taskId);
        task.setStatus(status);
        taskRepository.save(task);
        activityLogService.log("TASK_STATUS_CHANGED", "Task", taskId,
                currentUser, "Status changed to: " + status);
        return toResponse(task);
    }

    private Task findActiveOrThrow(String id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .priority(t.getPriority())
                .status(t.getStatus())
                .dueDate(t.getDueDate())
                .estimatedHours(t.getEstimatedHours())
                .actualHours(t.getActualHours())
                .projectId(t.getProject().getId())
                .projectName(t.getProject().getName())
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUserName(t.getAssignedUser() != null ? t.getAssignedUser().getFullName() : null)
                .reporterId(t.getReporter() != null ? t.getReporter().getId() : null)
                .reporterName(t.getReporter() != null ? t.getReporter().getFullName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
