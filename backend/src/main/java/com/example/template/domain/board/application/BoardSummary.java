package com.example.template.domain.board.application;

/** 사용자 화면용 게시판 요약 (MyBatis 조회 전용 DTO). */
public record BoardSummary(
        Long id,
        String slug,
        String name,
        String description,
        boolean commentEnabled,
        boolean attachmentEnabled,
        int sortOrder) {}
