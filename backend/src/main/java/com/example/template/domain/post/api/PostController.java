package com.example.template.domain.post.api;

import com.example.template.domain.post.api.PostResponses.PostDetailResponse;
import com.example.template.domain.post.api.PostResponses.PostListItemResponse;
import com.example.template.domain.post.application.PostCommand;
import com.example.template.domain.post.application.PostListResult;
import com.example.template.domain.post.application.PostService;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.response.PageRequestParams;
import com.example.template.global.response.PageResponse;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 API. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Post", description = "게시글 API")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    public record PostUpsertRequest(
            @NotBlank @Size(min = 1, max = 200) String title,
            @NotBlank String content,
            boolean notice,
            List<Long> fileIds,
            List<Long> inlineFileIds) {

        PostCommand toCommand() {
            return new PostCommand(title, content, notice, fileIds, inlineFileIds);
        }
    }

    public record PostListResponse(List<PostListItemResponse> notices, PageResponse<PostListItemResponse> page) {}

    @GetMapping("/boards/{slug}/posts")
    @Operation(summary = "게시글 목록", description = "공지는 첫 페이지 상단에 따로 반환한다.")
    public PostListResponse list(
            @PathVariable String slug,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        PageRequestParams params = PageRequestParams.of(page, size);
        PostListResult result =
                postService.getPosts(slug, roleOf(principal), searchType, keyword, params.page(), params.size());

        return new PostListResponse(
                result.notices().stream().map(PostListItemResponse::from).toList(),
                result.page().map(PostListItemResponse::from));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "게시글 상세", description = "조회수가 1 증가한다.")
    public PostDetailResponse detail(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        return PostDetailResponse.from(postService.getPostDetail(id, principal));
    }

    @PostMapping("/boards/{slug}/posts")
    @Operation(summary = "게시글 작성")
    public ResponseEntity<PostDetailResponse> create(
            @PathVariable String slug,
            @Valid @RequestBody PostUpsertRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Long postId = postService.create(slug, principal, request.toCommand()).getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PostDetailResponse.from(postService.getPostDetail(postId, principal)));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "게시글 수정", description = "작성자 본인 또는 관리자만 가능하다.")
    public PostDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PostUpsertRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        postService.update(id, principal, request.toCommand());
        return PostDetailResponse.from(postService.getPostDetail(id, principal));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "게시글 삭제", description = "작성자 본인 또는 관리자만 가능하다.")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        postService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    private static Role roleOf(AuthPrincipal principal) {
        return principal == null ? null : Role.valueOf(principal.role());
    }
}
