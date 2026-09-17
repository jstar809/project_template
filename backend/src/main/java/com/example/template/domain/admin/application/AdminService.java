package com.example.template.domain.admin.application;

import com.example.template.domain.admin.application.AdminDtos.AdminCommentItem;
import com.example.template.domain.admin.application.AdminDtos.AdminPostItem;
import com.example.template.domain.admin.application.AdminDtos.AdminUserItem;
import com.example.template.domain.admin.application.AdminDtos.CommentSearch;
import com.example.template.domain.admin.application.AdminDtos.Dashboard;
import com.example.template.domain.admin.application.AdminDtos.PostSearch;
import com.example.template.domain.admin.application.AdminDtos.UserSearch;
import com.example.template.domain.admin.domain.AdminErrorCode;
import com.example.template.domain.admin.infra.mybatis.AdminQueryMapper;
import com.example.template.domain.auth.application.AuthService;
import com.example.template.domain.comment.application.CommentService;
import com.example.template.domain.comment.domain.CommentStatus;
import com.example.template.domain.post.application.PostService;
import com.example.template.domain.post.domain.PostStatus;
import com.example.template.domain.user.application.UserService;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.error.BusinessException;
import com.example.template.global.response.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 기능.
 *
 * <p>조회는 {@link AdminQueryMapper}(MyBatis)가, 변경은 각 도메인의 application 서비스가 담당한다. 다른 도메인의 저장소를 직접
 * 건드리지 않는다(PRD ARCH-04).
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final int DASHBOARD_DAYS = 7;
    private static final int RECENT_POST_LIMIT = 5;

    private final AdminQueryMapper adminQueryMapper;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final AuthService authService;
    private final Clock clock;

    public AdminService(
            AdminQueryMapper adminQueryMapper,
            PostService postService,
            CommentService commentService,
            UserService userService,
            AuthService authService,
            Clock clock) {
        this.adminQueryMapper = adminQueryMapper;
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.authService = authService;
        this.clock = clock;
    }

    public Dashboard getDashboard() {
        Instant from = clock.instant().minus(Duration.ofDays(DASHBOARD_DAYS));
        return new Dashboard(
                adminQueryMapper.selectTotals(),
                adminQueryMapper.selectDailySignups(from),
                adminQueryMapper.selectDailyPosts(from),
                adminQueryMapper.selectRecentPosts(RECENT_POST_LIMIT));
    }

    // ---- 게시글 관리 ----

    public PageResponse<AdminPostItem> getPosts(PostSearch condition, int page, int size) {
        long total = adminQueryMapper.countPosts(condition);
        List<AdminPostItem> items = total == 0 ? List.of() : adminQueryMapper.selectPosts(condition);
        return PageResponse.of(items, page, size, total);
    }

    @Transactional
    public int changePostStatus(List<Long> postIds, PostStatus status) {
        return postService.changeStatusByAdmin(postIds, status);
    }

    @Transactional
    public int deletePosts(List<Long> postIds) {
        return postService.deleteByAdmin(postIds);
    }

    // ---- 댓글 관리 ----

    public PageResponse<AdminCommentItem> getComments(CommentSearch condition, int page, int size) {
        long total = adminQueryMapper.countComments(condition);
        List<AdminCommentItem> items = total == 0 ? List.of() : adminQueryMapper.selectComments(condition);
        return PageResponse.of(items, page, size, total);
    }

    @Transactional
    public int changeCommentStatus(List<Long> commentIds, CommentStatus status) {
        return commentService.changeStatusByAdmin(commentIds, status);
    }

    @Transactional
    public int deleteComments(List<Long> commentIds) {
        return commentService.deleteByAdmin(commentIds);
    }

    // ---- 사용자 관리 ----

    public PageResponse<AdminUserItem> getUsers(UserSearch condition, int page, int size) {
        long total = adminQueryMapper.countUsers(condition);
        List<AdminUserItem> items = total == 0 ? List.of() : adminQueryMapper.selectUsers(condition);
        return PageResponse.of(items, page, size, total);
    }

    /** 역할 변경. 자기 자신은 바꿀 수 없다(관리자가 0명이 되는 상황 방지). */
    @Transactional
    public void changeUserRole(Long actorId, Long targetUserId, Role role) {
        requireNotSelf(actorId, targetUserId);
        userService.changeRole(targetUserId, role);
        // 권한이 바뀌면 기존 토큰의 role 클레임이 낡은 값이므로 모두 폐기한다.
        authService.revokeAllTokens(targetUserId);
    }

    /** 계정 정지/해제. 정지하면 기존 세션을 즉시 끊는다. */
    @Transactional
    public void changeUserStatus(Long actorId, Long targetUserId, boolean suspend) {
        requireNotSelf(actorId, targetUserId);
        if (suspend) {
            userService.suspend(targetUserId);
            authService.revokeAllTokens(targetUserId);
        } else {
            userService.activate(targetUserId);
        }
    }

    private void requireNotSelf(Long actorId, Long targetUserId) {
        if (actorId != null && actorId.equals(targetUserId)) {
            throw new BusinessException(AdminErrorCode.ADMIN_SELF_MODIFICATION);
        }
    }
}
