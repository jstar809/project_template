package com.example.template.domain.comment.domain;

import java.util.List;
import java.util.Optional;

/** 댓글 저장소 포트. */
public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);

    /** 관리자 일괄 처리용 */
    List<Comment> findAllByIdIn(List<Long> ids);
}
