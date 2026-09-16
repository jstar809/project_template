package com.example.template.domain.comment.infra.jpa;

import com.example.template.domain.comment.domain.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 댓글 JPA 저장소. */
public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
