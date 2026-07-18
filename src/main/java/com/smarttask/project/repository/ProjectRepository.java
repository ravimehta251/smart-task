package com.smarttask.project.repository;

import com.smarttask.common.enums.ProjectStatus;
import com.smarttask.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Optional<Project> findByIdAndDeletedAtIsNull(String id);

    @Query("""
        SELECT p FROM Project p
        WHERE p.deletedAt IS NULL
        AND p.organization.id = :orgId
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR p.status = :status)
        """)
    Page<Project> searchProjects(
            @Param("orgId") String orgId,
            @Param("search") String search,
            @Param("status") ProjectStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Project p JOIN p.members m
        WHERE p.deletedAt IS NULL
        AND m.id = :userId
        """)
    Page<Project> findProjectsForUser(@Param("userId") String userId, Pageable pageable);
}
