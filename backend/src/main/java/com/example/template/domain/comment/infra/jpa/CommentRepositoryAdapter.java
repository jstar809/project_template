package com.example.template.domain.comment.infra.jpa;

import com.example.template.domain.comment.domain.Comment;
import com.example.template.domain.comment.domain.CommentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 댓글 포트를 JPA 구현과 연결한다. */
@Repository
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentJpaRepository jpaRepository;

    public CommentRepositoryAdapter(CommentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Comment save(Comment comment) {
        return jpaRepository.save(comment);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Comment> findAllByIdIn(List<Long> ids) {
        return jpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }
}
