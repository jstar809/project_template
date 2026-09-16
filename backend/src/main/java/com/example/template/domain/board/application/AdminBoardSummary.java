package com.example.template.domain.board.application;

import java.time.Instant;

/** 관리자 화면용 게시판 요약. 게시글 수를 함께 센다(MyBatis 조인). */
public record AdminBoardSummary(
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
        Instant createdAt) {}
