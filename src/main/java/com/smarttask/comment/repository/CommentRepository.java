package com.smarttask.comment.repository;

import com.smarttask.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    Optional<Comment> findByIdAndDeletedAtIsNull(String id);

    Page<Comment> findByTaskIdAndDeletedAtIsNull(String taskId, Pageable pageable);
}
