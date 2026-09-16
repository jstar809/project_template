package com.example.template.domain.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/** 게시글과 파일의 연결. 파일 하나는 게시글 하나에만 연결된다. */
@Entity
@Table(name = "post_files")
public class PostFile {

    @EmbeddedId
    private PostFileId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private UsageType usageType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PostFile() {}

    private PostFile(Long postId, Long fileId, UsageType usageType, int sortOrder) {
        this.id = new PostFileId(postId, fileId);
        this.usageType = usageType;
        this.sortOrder = sortOrder;
    }

    public static PostFile link(Long postId, Long fileId, UsageType usageType, int sortOrder) {
        return new PostFile(postId, fileId, usageType, sortOrder);
    }

    public Long getPostId() {
        return id.getPostId();
    }

    public Long getFileId() {
        return id.getFileId();
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /** 복합 키 (post_id, file_id) */
    @Embeddable
    public static class PostFileId implements Serializable {

        @Column(name = "post_id")
        private Long postId;

        @Column(name = "file_id")
        private Long fileId;

        protected PostFileId() {}

        PostFileId(Long postId, Long fileId) {
            this.postId = postId;
            this.fileId = fileId;
        }

        public Long getPostId() {
            return postId;
        }

        public Long getFileId() {
            return fileId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof PostFileId that)) return false;
            return Objects.equals(postId, that.postId) && Objects.equals(fileId, that.fileId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, fileId);
        }
    }
}
