package com.example.template.domain.comment.application;

import java.time.Instant;
import java.util.List;

/** 화면에 내려줄 댓글 트리 노드(대댓글은 replies에 담긴다). */
public record CommentNode(
        Long id,
        String content,
        Long authorId,
        String authorNickname,
        boolean deleted,
        boolean hidden,
        boolean canEdit,
        boolean canDelete,
        Instant createdAt,
        Instant updatedAt,
        List<CommentNode> replies) {}
