package com.smarttask.comment.controller;

import com.smarttask.comment.dto.CommentRequest;
import com.smarttask.comment.dto.CommentResponse;
import com.smarttask.comment.service.CommentService;
import com.smarttask.common.response.ApiResponse;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Task-level discussion threads")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String taskId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        commentService.addComment(taskId, request, SecurityUtils.getCurrentUser()),
                        "Comment added."));
    }

    @GetMapping
    @Operation(summary = "List all comments on a task")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getComments(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getTaskComments(taskId,
                        PageRequest.of(page, size, Sort.by("createdAt").ascending()))));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Edit a comment (author only)")
    public ResponseEntity<ApiResponse<CommentResponse>> editComment(
            @PathVariable String taskId,
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.editComment(commentId, request, SecurityUtils.getCurrentUser()),
                "Comment updated."));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment (author only)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String taskId,
            @PathVariable String commentId) {
        commentService.deleteComment(commentId, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.noContent("Comment deleted."));
    }
}
