package com.smarttask.attachment.repository;

import com.smarttask.attachment.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, String> {

    List<FileAttachment> findByTaskIdAndDeletedAtIsNull(String taskId);

    Optional<FileAttachment> findByIdAndDeletedAtIsNull(String id);
}
