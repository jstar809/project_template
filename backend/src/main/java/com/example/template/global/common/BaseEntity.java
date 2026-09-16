package com.example.template.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;

/**
 * 생성/수정 시각에 더해 작성자와 soft delete 시각을 갖는 공통 부모 클래스.
 *
 * <p>삭제는 행을 지우지 않고 {@code deletedAt}을 채우는 방식(soft delete)으로 한다.
 */
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** soft delete. 이미 삭제된 경우 시각을 덮어쓰지 않는다. */
    public void markDeleted(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
        }
    }

    public void restore() {
        deletedAt = null;
    }
}
