package com.example.template.domain.comment.domain;

import java.util.Optional;

/** 댓글 저장소 포트. */
public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);
}
