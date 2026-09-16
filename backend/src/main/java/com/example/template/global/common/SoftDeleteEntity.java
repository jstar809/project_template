package com.example.template.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * 생성/수정 시각에 더해 soft delete를 지원하는 공통 부모.
 *
 * <p>작성자(created_by)까지 필요한 엔티티는 {@link BaseEntity}를 상속한다. users 테이블처럼 작성자 개념이 없는 엔티티는 이 클래스를
 * 상속한다.
 */
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseTimeEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

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
