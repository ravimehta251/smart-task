package com.smarttask.organization.repository;

import com.smarttask.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByIdAndDeletedAtIsNull(String id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    @Query("""
        SELECT o FROM Organization o
        WHERE o.deletedAt IS NULL
        AND (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Organization> findAllActive(@Param("search") String search, Pageable pageable);
}
