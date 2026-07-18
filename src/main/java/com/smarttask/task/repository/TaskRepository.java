package com.smarttask.task.repository;

import com.smarttask.common.enums.TaskPriority;
import com.smarttask.common.enums.TaskStatus;
import com.smarttask.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    Optional<Task> findByIdAndDeletedAtIsNull(String id);

    @Query("""
        SELECT t FROM Task t
        WHERE t.deletedAt IS NULL
        AND t.project.id = :projectId
        AND (:search   IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status   IS NULL OR t.status   = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        """)
    Page<Task> searchTasks(
            @Param("projectId") String projectId,
            @Param("search")    String search,
            @Param("status")    TaskStatus status,
            @Param("priority")  TaskPriority priority,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.deletedAt IS NULL
        AND t.assignedUser.id = :userId
        AND (:status IS NULL OR t.status = :status)
        """)
    Page<Task> findTasksAssignedToUser(
            @Param("userId") String userId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );

    long countByProjectIdAndDeletedAtIsNull(String projectId);

    long countByProjectIdAndStatusAndDeletedAtIsNull(String projectId, TaskStatus status);
}
