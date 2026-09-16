package com.example.template.domain.comment.domain;

import com.example.template.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 댓글. 대댓글은 1단계까지만 허용한다(parent가 있는 댓글에는 다시 답글을 달 수 없다).
 *
 * <p>내용은 plain text로 저장하고 화면에서 이스케이프해 보여준다(PRD FR-COMMENT-05).
 */
@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    protected Comment() {}

    private Comment(Long postId, Long authorId, Long parentId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.status = CommentStatus.PUBLISHED;
    }

    public static Comment create(Long postId, Long authorId, Long parentId, String content) {
        return new Comment(postId, authorId, parentId, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void hide() {
        this.status = CommentStatus.HIDDEN;
    }

    public void publish() {
        this.status = CommentStatus.PUBLISHED;
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isAuthor(Long userId) {
        return userId != null && authorId.equals(userId);
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getContent() {
        return content;
    }

    public CommentStatus getStatus() {
        return status;
    }
}
