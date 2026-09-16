package com.example.template.domain.comment.application;

import java.time.Instant;

/**
 * 댓글 조회 결과 (MyBatis 전용 DTO).
 *
 * <p>삭제되거나 숨겨진 댓글도 자리는 남긴다. 대댓글이 달려 있으면 구조가 깨지기 때문이다. 대신 내용은 서비스에서 비운다.
 */
public record CommentItem(
        Long id,
        Long parentId,
        String content,
        Long authorId,
        String authorNickname,
        boolean deleted,
        boolean hidden,
        Instant createdAt,
        Instant updatedAt) {}
