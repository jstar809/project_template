package com.example.template.domain.post.domain;

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
 * 게시글.
 *
 * <p>본문은 DB마다 타입이 다르다(PostgreSQL TEXT / MySQL LONGTEXT). 벤더 전용 columnDefinition 대신
 * 큰 length 값을 줘서 두 DB에서 모두 validate가 통과하게 한다(PRD DB-04).
 */
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    // length를 크게 주면 Hibernate가 DB별 긴 텍스트 타입을 고른다
    // (PostgreSQL: text, MySQL: longtext). 벤더 전용 columnDefinition을 쓰지 않는 방법이다(PRD DB-04).
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @Column(name = "content_text", nullable = false, length = Integer.MAX_VALUE)
    private String contentText;

    @Column(name = "is_notice", nullable = false)
    private boolean notice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    protected Post() {}

    private Post(Long boardId, Long authorId, String title, String content, String contentText, boolean notice) {
        this.boardId = boardId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.contentText = contentText;
        this.notice = notice;
        this.status = PostStatus.PUBLISHED;
        this.viewCount = 0;
        this.commentCount = 0;
    }

    public static Post create(
            Long boardId, Long authorId, String title, String content, String contentText, boolean notice) {
        return new Post(boardId, authorId, title, content, contentText, notice);
    }

    public void update(String title, String content, String contentText, boolean notice) {
        this.title = title;
        this.content = content;
        this.contentText = contentText;
        this.notice = notice;
    }

    public void hide() {
        this.status = PostStatus.HIDDEN;
    }

    public void publish() {
        this.status = PostStatus.PUBLISHED;
    }

    public boolean isVisible() {
        return status == PostStatus.PUBLISHED && !isDeleted();
    }

    /** 작성자 본인인지 확인한다. */
    public boolean isAuthor(Long userId) {
        return userId != null && authorId.equals(userId);
    }

    public Long getId() {
        return id;
    }

    public Long getBoardId() {
        return boardId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getContentText() {
        return contentText;
    }

    public boolean isNotice() {
        return notice;
    }

    public PostStatus getStatus() {
        return status;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getCommentCount() {
        return commentCount;
    }
}
