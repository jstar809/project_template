package com.example.template.domain.admin.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 관리자 화면 조회 전용 DTO 모음 (MyBatis 결과). */
public final class AdminDtos {

    private AdminDtos() {}

    /** 대시보드 상단 숫자 */
    public record Totals(long users, long posts, long comments) {}

    /** 일별 추이 한 점 */
    public record DailyCount(LocalDate date, long count) {}

    /** 대시보드 최근 게시글 */
    public record RecentPost(Long id, String title, String boardName, String authorNickname, Instant createdAt) {}

    /** 대시보드 전체 */
    public record Dashboard(
            Totals totals, List<DailyCount> dailySignups, List<DailyCount> dailyPosts, List<RecentPost> recentPosts) {}

    /** 관리자 게시글 목록 항목 */
    public record AdminPostItem(
            Long id,
            String title,
            String boardSlug,
            String boardName,
            String authorNickname,
            String status,
            boolean deleted,
            long viewCount,
            long commentCount,
            Instant createdAt) {}

    /** 관리자 댓글 목록 항목 */
    public record AdminCommentItem(
            Long id,
            Long postId,
            String postTitle,
            String content,
            String authorNickname,
            String status,
            boolean deleted,
            Instant createdAt) {}

    /** 관리자 사용자 목록 항목 */
    public record AdminUserItem(
            Long id,
            String email,
            String nickname,
            String role,
            String status,
            long postCount,
            Instant lastLoginAt,
            Instant createdAt) {}

    /** 목록 검색 조건 (게시글) */
    public record PostSearch(
            Long boardId, String status, String keyword, Instant from, Instant to, int offset, int limit) {}

    /** 목록 검색 조건 (댓글) */
    public record CommentSearch(String status, String keyword, int offset, int limit) {}

    /** 목록 검색 조건 (사용자) */
    public record UserSearch(String keyword, String role, String status, int offset, int limit) {}
}
