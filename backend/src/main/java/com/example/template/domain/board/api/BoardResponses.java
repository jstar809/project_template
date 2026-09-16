package com.example.template.domain.board.api;

import com.example.template.domain.board.application.AdminBoardSummary;
import com.example.template.domain.board.application.BoardSummary;
import com.example.template.domain.board.domain.Board;
import com.example.template.domain.user.domain.Role;
import java.time.Instant;

/** 게시판 응답 DTO 모음. */
public final class BoardResponses {

    private BoardResponses() {}

    /** 사용자 화면 목록용 */
    public record BoardSummaryResponse(
            Long id, String slug, String name, String description, boolean commentEnabled, boolean attachmentEnabled) {

        public static BoardSummaryResponse from(BoardSummary summary) {
            return new BoardSummaryResponse(
                    summary.id(),
                    summary.slug(),
                    summary.name(),
                    summary.description(),
                    summary.commentEnabled(),
                    summary.attachmentEnabled());
        }
    }

    /** 사용자 화면 상세용. canWrite로 글쓰기 버튼 노출을 결정한다. */
    public record BoardDetailResponse(
            Long id,
            String slug,
            String name,
            String description,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean canWrite) {

        public static BoardDetailResponse from(Board board, Role role) {
            return new BoardDetailResponse(
                    board.getId(),
                    board.getSlug(),
                    board.getName(),
                    board.getDescription(),
                    board.isCommentEnabled(),
                    board.isAttachmentEnabled(),
                    board.canWrite(role));
        }
    }

    /** 관리자 화면용 */
    public record AdminBoardResponse(
            Long id,
            String slug,
            String name,
            String description,
            String readPermission,
            String writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean active,
            int sortOrder,
            long postCount,
            Instant createdAt) {

        public static AdminBoardResponse from(AdminBoardSummary summary) {
            return new AdminBoardResponse(
                    summary.id(),
                    summary.slug(),
                    summary.name(),
                    summary.description(),
                    summary.readPermission(),
                    summary.writePermission(),
                    summary.commentEnabled(),
                    summary.attachmentEnabled(),
                    summary.active(),
                    summary.sortOrder(),
                    summary.postCount(),
                    summary.createdAt());
        }

        public static AdminBoardResponse from(Board board, long postCount) {
            return new AdminBoardResponse(
                    board.getId(),
                    board.getSlug(),
                    board.getName(),
                    board.getDescription(),
                    board.getReadPermission().name(),
                    board.getWritePermission().name(),
                    board.isCommentEnabled(),
                    board.isAttachmentEnabled(),
                    board.isActive(),
                    board.getSortOrder(),
                    postCount,
                    board.getCreatedAt());
        }
    }
}
