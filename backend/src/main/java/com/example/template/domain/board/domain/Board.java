package com.example.template.domain.board.domain;

import com.example.template.domain.user.domain.Role;
import com.example.template.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/** 게시판. 관리자가 만들고 권한과 기능(댓글·첨부)을 설정한다. */
@Entity
@Table(name = "boards")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String slug;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_permission", nullable = false, length = 20)
    private Permission readPermission;

    @Enumerated(EnumType.STRING)
    @Column(name = "write_permission", nullable = false, length = 20)
    private Permission writePermission;

    @Column(name = "comment_enabled", nullable = false)
    private boolean commentEnabled;

    @Column(name = "attachment_enabled", nullable = false)
    private boolean attachmentEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Board() {}

    private Board(
            String slug,
            String name,
            String description,
            Permission readPermission,
            Permission writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean active,
            int sortOrder) {
        this.slug = normalizeSlug(slug);
        this.name = name;
        this.description = description;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.commentEnabled = commentEnabled;
        this.attachmentEnabled = attachmentEnabled;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public static Board create(
            String slug,
            String name,
            String description,
            Permission readPermission,
            Permission writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean active,
            int sortOrder) {
        return new Board(
                slug,
                name,
                description,
                readPermission,
                writePermission,
                commentEnabled,
                attachmentEnabled,
                active,
                sortOrder);
    }

    public static String normalizeSlug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase(Locale.ROOT);
    }

    public void update(
            String slug,
            String name,
            String description,
            Permission readPermission,
            Permission writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean active) {
        this.slug = normalizeSlug(slug);
        this.name = name;
        this.description = description;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.commentEnabled = commentEnabled;
        this.attachmentEnabled = attachmentEnabled;
        this.active = active;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * soft delete.
     *
     * <p>slug에 유니크 제약이 있어 그대로 두면 같은 slug로 새 게시판을 만들 수 없다. 그래서 삭제 표시와 함께 slug를 바꿔 자리를 비워준다(PRD 11.3).
     */
    public void softDelete(Instant now) {
        markDeleted(now);
        this.active = false;
        this.slug = this.slug + "__deleted__" + this.id;
    }

    /** 읽기 권한 확인. 비활성 게시판은 관리자만 볼 수 있다. */
    public boolean canRead(Role role) {
        if (!active && role != Role.ADMIN) {
            return false;
        }
        return readPermission.allows(role);
    }

    /** 쓰기 권한 확인. */
    public boolean canWrite(Role role) {
        if (!active && role != Role.ADMIN) {
            return false;
        }
        return writePermission.allows(role);
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Permission getReadPermission() {
        return readPermission;
    }

    public Permission getWritePermission() {
        return writePermission;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public boolean isAttachmentEnabled() {
        return attachmentEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
