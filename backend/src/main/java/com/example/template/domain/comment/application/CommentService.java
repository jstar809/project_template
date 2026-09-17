package com.example.template.domain.comment.application;

import com.example.template.domain.board.application.BoardService;
import com.example.template.domain.board.domain.Board;
import com.example.template.domain.comment.domain.Comment;
import com.example.template.domain.comment.domain.CommentErrorCode;
import com.example.template.domain.comment.domain.CommentRepository;
import com.example.template.domain.comment.domain.CommentStatus;
import com.example.template.domain.comment.infra.mybatis.CommentQueryMapper;
import com.example.template.domain.post.application.PostService;
import com.example.template.domain.post.domain.Post;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.error.BusinessException;
import com.example.template.global.security.AuthPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 업무 로직. 목록은 MyBatis, 쓰기는 JPA. */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final String DELETED_MESSAGE = "삭제된 댓글입니다.";
    private static final String HIDDEN_MESSAGE = "관리자에 의해 숨겨진 댓글입니다.";

    private final CommentRepository commentRepository;
    private final CommentQueryMapper commentQueryMapper;
    private final PostService postService;
    private final BoardService boardService;
    private final Clock clock;

    public CommentService(
            CommentRepository commentRepository,
            CommentQueryMapper commentQueryMapper,
            PostService postService,
            BoardService boardService,
            Clock clock) {
        this.commentRepository = commentRepository;
        this.commentQueryMapper = commentQueryMapper;
        this.postService = postService;
        this.boardService = boardService;
        this.clock = clock;
    }

    /** 댓글 목록을 1단계 트리로 만들어 돌려준다. */
    public List<CommentNode> getComments(Long postId, AuthPrincipal principal) {
        Role role = roleOf(principal);
        postService.getVisiblePost(postId, role);

        List<CommentItem> items = commentQueryMapper.selectCommentsByPostId(postId);
        Long currentUserId = principal == null ? null : principal.userId();
        boolean isAdmin = role == Role.ADMIN;

        Map<Long, CommentNode> roots = new LinkedHashMap<>();
        for (CommentItem item : items) {
            CommentNode node = toNode(item, currentUserId, isAdmin);
            if (item.parentId() == null) {
                roots.put(item.id(), node);
            } else {
                CommentNode parent = roots.get(item.parentId());
                if (parent != null) {
                    parent.replies().add(node);
                }
            }
        }
        return new ArrayList<>(roots.values());
    }

    @Transactional
    public Comment create(Long postId, AuthPrincipal principal, Long parentId, String content) {
        Role role = roleOf(principal);
        Post post = postService.getVisiblePost(postId, role);
        Board board = boardService.getById(post.getBoardId());

        if (!board.isCommentEnabled()) {
            throw new BusinessException(CommentErrorCode.COMMENT_DISABLED);
        }
        if (parentId != null) {
            validateParent(parentId, postId);
        }

        Comment comment = commentRepository.save(Comment.create(postId, principal.userId(), parentId, content));
        postService.addCommentCount(postId, 1);
        return comment;
    }

    @Transactional
    public Comment update(Long commentId, AuthPrincipal principal, String content) {
        Comment comment = getComment(commentId);
        requireEditPermission(comment, principal);
        comment.updateContent(content);
        return comment;
    }

    @Transactional
    public void delete(Long commentId, AuthPrincipal principal) {
        Comment comment = getComment(commentId);
        requireEditPermission(comment, principal);
        comment.markDeleted(clock.instant());
        postService.addCommentCount(comment.getPostId(), -1);
    }

    /** 관리자 일괄 상태 변경(숨김/복구). */
    @Transactional
    public int changeStatusByAdmin(List<Long> commentIds, CommentStatus status) {
        List<Comment> comments = commentRepository.findAllByIdIn(commentIds);
        comments.forEach(comment -> {
            if (status == CommentStatus.HIDDEN) {
                comment.hide();
            } else {
                comment.publish();
            }
        });
        return comments.size();
    }

    /** 관리자 일괄 삭제(soft delete). 게시글의 댓글 수도 함께 줄인다. */
    @Transactional
    public int deleteByAdmin(List<Long> commentIds) {
        List<Comment> comments = commentRepository.findAllByIdIn(commentIds);
        Instant now = clock.instant();
        comments.forEach(comment -> {
            comment.markDeleted(now);
            postService.addCommentCount(comment.getPostId(), -1);
        });
        return comments.size();
    }

    private void validateParent(Long parentId, Long postId) {
        Comment parent = getComment(parentId);
        if (!parent.getPostId().equals(postId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_PARENT_MISMATCH);
        }
        // 대댓글에 다시 답글을 달면 트리가 깊어져 화면과 쿼리가 복잡해진다. 1단계로 제한한다.
        if (parent.isReply()) {
            throw new BusinessException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
    }

    private Comment getComment(Long commentId) {
        return commentRepository
                .findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void requireEditPermission(Comment comment, AuthPrincipal principal) {
        boolean isOwner = principal != null && comment.isAuthor(principal.userId());
        boolean isAdmin = roleOf(principal) == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(CommentErrorCode.COMMENT_EDIT_FORBIDDEN);
        }
    }

    /** 삭제/숨김 댓글은 자리만 남기고 내용을 감춘다. */
    private CommentNode toNode(CommentItem item, Long currentUserId, boolean isAdmin) {
        boolean masked = item.deleted() || item.hidden();
        String content = item.deleted() ? DELETED_MESSAGE : (item.hidden() ? HIDDEN_MESSAGE : item.content());
        boolean canEdit = !masked && (isAdmin || (currentUserId != null && currentUserId.equals(item.authorId())));

        return new CommentNode(
                item.id(),
                content,
                masked ? null : item.authorId(),
                masked ? null : item.authorNickname(),
                item.deleted(),
                item.hidden(),
                canEdit,
                canEdit,
                item.createdAt(),
                item.updatedAt(),
                new ArrayList<>());
    }

    private static Role roleOf(AuthPrincipal principal) {
        return principal == null ? null : Role.valueOf(principal.role());
    }
}
