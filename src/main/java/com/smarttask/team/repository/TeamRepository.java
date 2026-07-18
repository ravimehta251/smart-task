package com.smarttask.team.repository;

import com.smarttask.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {

    Optional<Team> findByIdAndDeletedAtIsNull(String id);

    boolean existsByNameAndOrganizationIdAndDeletedAtIsNull(String name, String organizationId);

    @Query("""
        SELECT t FROM Team t
        WHERE t.deletedAt IS NULL
        AND t.organization.id = :orgId
        AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Team> findByOrganization(@Param("orgId") String orgId,
                                   @Param("search") String search,
                                   Pageable pageable);
}
