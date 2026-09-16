package com.example.template.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;

/**
 * 생성/수정 시각 + soft delete + 작성자를 갖는 공통 부모 클래스.
 *
 * <p>boards, posts, comments 처럼 "누가 만들었는지"를 남기는 엔티티가 상속한다.
 */
@MappedSuperclass
public abstract class BaseEntity extends SoftDeleteEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    public Long getCreatedBy() {
        return createdBy;
    }
}
