package com.example.template.domain.post.application;

import com.example.template.domain.board.application.BoardService;
import com.example.template.domain.board.domain.Board;
import com.example.template.domain.file.application.FileService;
import com.example.template.domain.file.domain.FileErrorCode;
import com.example.template.domain.post.domain.Post;
import com.example.template.domain.post.domain.PostErrorCode;
import com.example.template.domain.post.domain.PostRepository;
import com.example.template.domain.post.domain.PostStatus;
import com.example.template.domain.post.infra.mybatis.PostQueryMapper;
import com.example.template.domain.user.application.UserService;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.error.BusinessException;
import com.example.template.global.html.HtmlSanitizer;
import com.example.template.global.response.PageResponse;
import com.example.template.global.security.AuthPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 업무 로직.
 *
 * <p>쓰기는 JPA, 목록·검색은 MyBatis를 쓴다(PRD 8.2).
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    /** 본문 HTML 최대 길이 (약 100KB) */
    private static final int MAX_CONTENT_LENGTH = 100_000;

    private final PostRepository postRepository;
    private final PostQueryMapper postQueryMapper;
    private final BoardService boardService;
    private final FileService fileService;
    private final UserService userService;
    private final HtmlSanitizer htmlSanitizer;
    private final Clock clock;

    public PostService(
            PostRepository postRepository,
            PostQueryMapper postQueryMapper,
            BoardService boardService,
            FileService fileService,
            UserService userService,
            HtmlSanitizer htmlSanitizer,
            Clock clock) {
        this.postRepository = postRepository;
        this.postQueryMapper = postQueryMapper;
        this.boardService = boardService;
        this.fileService = fileService;
        this.userService = userService;
        this.htmlSanitizer = htmlSanitizer;
        this.clock = clock;
    }

    /** 목록 조회: 공지는 페이지와 무관하게 상단에 따로 돌려준다. */
    public PostListResult getPosts(String boardSlug, Role role, String searchType, String keyword, int page, int size) {
        Board board = boardService.getReadableBoard(boardSlug, role);

        PostSearchCondition condition = new PostSearchCondition(board.getId(), searchType, keyword, page * size, size);
        long total = postQueryMapper.countPosts(condition);
        List<PostListItem> posts = total == 0 ? List.of() : postQueryMapper.selectPosts(condition);
        List<PostListItem> notices = page == 0 ? postQueryMapper.selectNotices(board.getId()) : List.of();

        return new PostListResult(notices, PageResponse.of(posts, page, size, total));
    }

    /** 상세 조회. 읽기 권한을 확인하고 조회수를 원자적으로 올린다. */
    @Transactional
    public PostDetail getPostDetail(Long postId, AuthPrincipal principal) {
        Role role = roleOf(principal);
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        Board board = boardService.getReadableBoard(boardSlugOf(post), role);

        boolean isAdmin = role == Role.ADMIN;
        if (!post.isVisible() && !isAdmin) {
            // 숨김/삭제된 글은 존재 자체를 알리지 않는다.
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }

        postRepository.increaseViewCount(postId);

        boolean canEdit = isAdmin || post.isAuthor(principal == null ? null : principal.userId());
        String authorNickname = userService.getById(post.getAuthorId()).getNickname();
        return new PostDetail(post, board, authorNickname, fileService.getAttachmentsOfPost(postId), canEdit, canEdit);
    }

    @Transactional
    public Post create(String boardSlug, AuthPrincipal principal, PostCommand command) {
        Role role = roleOf(principal);
        Board board = boardService.getWritableBoard(boardSlug, role);
        validateNotice(command.notice(), role);
        validateAttachments(board, command);

        String sanitized = sanitize(command.content());
        Post post = postRepository.save(Post.create(
                board.getId(),
                principal.userId(),
                command.title(),
                sanitized,
                htmlSanitizer.toPlainText(sanitized),
                command.notice()));

        fileService.linkToPost(post.getId(), principal.userId(), command.fileIds(), command.inlineFileIds());
        return post;
    }

    @Transactional
    public Post update(Long postId, AuthPrincipal principal, PostCommand command) {
        Role role = roleOf(principal);
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        Board board = boardService.getById(post.getBoardId());

        requireEditPermission(post, principal, role);
        validateNotice(command.notice(), role);
        validateAttachments(board, command);

        String sanitized = sanitize(command.content());
        post.update(command.title(), sanitized, htmlSanitizer.toPlainText(sanitized), command.notice());

        fileService.relinkToPost(postId, post.getAuthorId(), command.fileIds(), command.inlineFileIds());
        return post;
    }

    @Transactional
    public void delete(Long postId, AuthPrincipal principal) {
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        requireEditPermission(post, principal, roleOf(principal));
        post.markDeleted(clock.instant());
    }

    /** 댓글 도메인에서 호출한다. 게시글 존재와 읽기 권한을 함께 확인한다. */
    public Post getVisiblePost(Long postId, Role role) {
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        boardService.getReadableBoard(boardSlugOf(post), role);
        if (!post.isVisible() && role != Role.ADMIN) {
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    public void addCommentCount(Long postId, int delta) {
        postRepository.addCommentCount(postId, delta);
    }

    public long countByBoard(Long boardId) {
        return postRepository.countByBoardId(boardId);
    }

    /**
     * 관리자 일괄 상태 변경(숨김/복구).
     *
     * <p>권한 검사는 SecurityConfig가 ADMIN으로 제한하므로 여기서는 하지 않는다.
     */
    @Transactional
    public int changeStatusByAdmin(List<Long> postIds, PostStatus status) {
        List<Post> posts = postRepository.findAllByIdIn(postIds);
        posts.forEach(post -> {
            if (status == PostStatus.HIDDEN) {
                post.hide();
            } else {
                post.publish();
            }
        });
        return posts.size();
    }

    /** 관리자 일괄 삭제(soft delete). */
    @Transactional
    public int deleteByAdmin(List<Long> postIds) {
        List<Post> posts = postRepository.findAllByIdIn(postIds);
        Instant now = clock.instant();
        posts.forEach(post -> post.markDeleted(now));
        return posts.size();
    }

    private void requireEditPermission(Post post, AuthPrincipal principal, Role role) {
        boolean isOwner = principal != null && post.isAuthor(principal.userId());
        if (!isOwner && role != Role.ADMIN) {
            throw new BusinessException(PostErrorCode.POST_EDIT_FORBIDDEN);
        }
    }

    private void validateNotice(boolean notice, Role role) {
        if (notice && role != Role.ADMIN) {
            throw new BusinessException(PostErrorCode.POST_NOTICE_FORBIDDEN);
        }
    }

    private void validateAttachments(Board board, PostCommand command) {
        boolean hasFiles = command.fileIds() != null && !command.fileIds().isEmpty();
        if (hasFiles && !board.isAttachmentEnabled()) {
            throw new BusinessException(FileErrorCode.FILE_ATTACHMENT_DISABLED);
        }
    }

    private String sanitize(String rawContent) {
        String sanitized = htmlSanitizer.sanitize(rawContent);
        if (sanitized.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(PostErrorCode.POST_CONTENT_TOO_LARGE);
        }
        return sanitized;
    }

    private String boardSlugOf(Post post) {
        return boardService.getById(post.getBoardId()).getSlug();
    }

    private static Role roleOf(AuthPrincipal principal) {
        return principal == null ? null : Role.valueOf(principal.role());
    }
}
