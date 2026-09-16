package com.example.template.domain.comment.api;

import com.example.template.domain.comment.application.CommentNode;
import com.example.template.domain.comment.application.CommentService;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 댓글 API. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comment", description = "댓글 API")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    public record CommentCreateRequest(
            @NotBlank @Size(min = 1, max = 1000) String content, Long parentId) {}

    public record CommentUpdateRequest(
            @NotBlank @Size(min = 1, max = 1000) String content) {}

    public record CommentResponse(
            Long id,
            String content,
            Long authorId,
            String authorNickname,
            boolean deleted,
            boolean hidden,
            boolean canEdit,
            boolean canDelete,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            List<CommentResponse> replies) {

        static CommentResponse from(CommentNode node) {
            return new CommentResponse(
                    node.id(),
                    node.content(),
                    node.authorId(),
                    node.authorNickname(),
                    node.deleted(),
                    node.hidden(),
                    node.canEdit(),
                    node.canDelete(),
                    node.createdAt(),
                    node.updatedAt(),
                    node.replies().stream().map(CommentResponse::from).toList());
        }
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 목록", description = "대댓글은 replies에 담긴다(1단계).")
    public List<CommentResponse> list(@PathVariable Long postId, @AuthenticationPrincipal AuthPrincipal principal) {
        return commentService.getComments(postId, principal).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 작성")
    public ResponseEntity<Void> create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        commentService.create(postId, principal, request.parentId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "댓글 수정")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        commentService.update(id, principal, request.content());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "댓글 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        commentService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
