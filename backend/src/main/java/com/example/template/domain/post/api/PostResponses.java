package com.example.template.domain.post.api;

import com.example.template.domain.file.domain.StoredFile;
import com.example.template.domain.post.application.PostDetail;
import com.example.template.domain.post.application.PostListItem;
import java.time.Instant;
import java.util.List;

/** 게시글 응답 DTO 모음. */
public final class PostResponses {

    private PostResponses() {}

    public record PostListItemResponse(
            Long id,
            String title,
            String authorNickname,
            boolean notice,
            long viewCount,
            long commentCount,
            boolean hasAttachment,
            Instant createdAt) {

        public static PostListItemResponse from(PostListItem item) {
            return new PostListItemResponse(
                    item.id(),
                    item.title(),
                    item.authorNickname(),
                    item.notice(),
                    item.viewCount(),
                    item.commentCount(),
                    item.hasAttachment(),
                    item.createdAt());
        }
    }

    public record AttachmentResponse(Long id, String url, String originalName, String contentType, long size) {

        public static AttachmentResponse from(StoredFile file) {
            return new AttachmentResponse(
                    file.getId(),
                    "/api/v1/files/" + file.getId(),
                    file.getOriginalName(),
                    file.getContentType(),
                    file.getSizeBytes());
        }
    }

    public record BoardRef(String slug, String name) {}

    public record AuthorRef(Long id, String nickname) {}

    public record PostDetailResponse(
            Long id,
            BoardRef board,
            String title,
            String content,
            AuthorRef author,
            boolean notice,
            long viewCount,
            long commentCount,
            List<AttachmentResponse> attachments,
            boolean canEdit,
            boolean canDelete,
            Instant createdAt,
            Instant updatedAt) {

        public static PostDetailResponse from(PostDetail detail) {
            return new PostDetailResponse(
                    detail.post().getId(),
                    new BoardRef(detail.board().getSlug(), detail.board().getName()),
                    detail.post().getTitle(),
                    detail.post().getContent(),
                    new AuthorRef(detail.post().getAuthorId(), detail.authorNickname()),
                    detail.post().isNotice(),
                    detail.post().getViewCount(),
                    detail.post().getCommentCount(),
                    detail.attachments().stream().map(AttachmentResponse::from).toList(),
                    detail.canEdit(),
                    detail.canDelete(),
                    detail.post().getCreatedAt(),
                    detail.post().getUpdatedAt());
        }
    }
}
