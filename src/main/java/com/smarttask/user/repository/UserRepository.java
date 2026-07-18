package com.smarttask.user.repository;

import com.smarttask.common.enums.Role;
import com.smarttask.common.enums.UserStatus;
import com.smarttask.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.deletedAt IS NULL
        AND u.organization.id = :orgId
        AND (:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.status = :status)
        """)
    Page<User> searchUsers(
            @Param("orgId") String orgId,
            @Param("search") String search,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    Optional<User> findByIdAndDeletedAtIsNull(String id);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}
