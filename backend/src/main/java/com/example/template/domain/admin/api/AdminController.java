package com.example.template.domain.admin.api;

import com.example.template.domain.admin.application.AdminDtos.AdminCommentItem;
import com.example.template.domain.admin.application.AdminDtos.AdminPostItem;
import com.example.template.domain.admin.application.AdminDtos.AdminUserItem;
import com.example.template.domain.admin.application.AdminDtos.CommentSearch;
import com.example.template.domain.admin.application.AdminDtos.Dashboard;
import com.example.template.domain.admin.application.AdminDtos.PostSearch;
import com.example.template.domain.admin.application.AdminDtos.UserSearch;
import com.example.template.domain.admin.application.AdminService;
import com.example.template.domain.comment.domain.CommentStatus;
import com.example.template.domain.post.domain.PostStatus;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.response.PageRequestParams;
import com.example.template.global.response.PageResponse;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 API (대시보드, 게시글·댓글·사용자 관리).
 *
 * <p>게시판 관리는 {@code AdminBoardController}에 따로 있다. 접근 제한은 SecurityConfig에서 {@code
 * /api/v1/admin/**} → ADMIN 으로 처리한다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    public record StatusChangeRequest(
            @NotEmpty List<Long> ids, @NotNull String status) {}

    public record IdsRequest(@NotEmpty List<Long> ids) {}

    public record RoleChangeRequest(@NotNull Role role) {}

    public record UserStatusRequest(@NotNull String status) {}

    @GetMapping("/dashboard")
    @Operation(summary = "대시보드", description = "전체 통계와 최근 7일 추이, 최근 게시글을 반환한다.")
    public Dashboard dashboard() {
        return adminService.getDashboard();
    }

    // ---- 게시글 ----

    @GetMapping("/posts")
    @Operation(summary = "게시글 목록(관리자)", description = "숨김·삭제된 글도 포함한다.")
    public PageResponse<AdminPostItem> posts(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageRequestParams params = PageRequestParams.of(page, size);
        PostSearch condition = new PostSearch(
                boardId, emptyToNull(status), emptyToNull(keyword), from, to, params.offset(), params.size());
        return adminService.getPosts(condition, params.page(), params.size());
    }

    @PatchMapping("/posts/status")
    @Operation(summary = "게시글 숨김/복구", description = "status: PUBLISHED 또는 HIDDEN")
    public ResponseEntity<Void> changePostStatus(@Valid @RequestBody StatusChangeRequest request) {
        adminService.changePostStatus(request.ids(), PostStatus.valueOf(request.status()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts")
    @Operation(summary = "게시글 일괄 삭제")
    public ResponseEntity<Void> deletePosts(@Valid @RequestBody IdsRequest request) {
        adminService.deletePosts(request.ids());
        return ResponseEntity.noContent().build();
    }

    // ---- 댓글 ----

    @GetMapping("/comments")
    @Operation(summary = "댓글 목록(관리자)")
    public PageResponse<AdminCommentItem> comments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageRequestParams params = PageRequestParams.of(page, size);
        CommentSearch condition =
                new CommentSearch(emptyToNull(status), emptyToNull(keyword), params.offset(), params.size());
        return adminService.getComments(condition, params.page(), params.size());
    }

    @PatchMapping("/comments/status")
    @Operation(summary = "댓글 숨김/복구")
    public ResponseEntity<Void> changeCommentStatus(@Valid @RequestBody StatusChangeRequest request) {
        adminService.changeCommentStatus(request.ids(), CommentStatus.valueOf(request.status()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments")
    @Operation(summary = "댓글 일괄 삭제")
    public ResponseEntity<Void> deleteComments(@Valid @RequestBody IdsRequest request) {
        adminService.deleteComments(request.ids());
        return ResponseEntity.noContent().build();
    }

    // ---- 사용자 ----

    @GetMapping("/users")
    @Operation(summary = "사용자 목록(관리자)")
    public PageResponse<AdminUserItem> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageRequestParams params = PageRequestParams.of(page, size);
        UserSearch condition = new UserSearch(
                emptyToNull(keyword), emptyToNull(role), emptyToNull(status), params.offset(), params.size());
        return adminService.getUsers(condition, params.page(), params.size());
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "사용자 역할 변경", description = "자기 자신은 변경할 수 없다. 변경 시 해당 사용자의 토큰을 폐기한다.")
    public ResponseEntity<Void> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleChangeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        adminService.changeUserRole(principal.userId(), id, request.role());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "사용자 정지/해제", description = "status: ACTIVE 또는 SUSPENDED. 자기 자신은 변경할 수 없다.")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        adminService.changeUserStatus(principal.userId(), id, "SUSPENDED".equals(request.status()));
        return ResponseEntity.noContent().build();
    }

    /** 쿼리 파라미터가 빈 문자열이면 조건 없음으로 취급한다. */
    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
