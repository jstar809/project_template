package com.example.template.domain.post.application;

import java.time.Instant;

/** 게시글 목록 항목 (MyBatis 조회 전용 DTO). 조인 한 번으로 필요한 값을 모두 채운다. */
public record PostListItem(
        Long id,
        String title,
        Long authorId,
        String authorNickname,
        boolean notice,
        long viewCount,
        long commentCount,
        boolean hasAttachment,
        Instant createdAt) {}
