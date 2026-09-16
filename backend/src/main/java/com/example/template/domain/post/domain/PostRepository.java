package com.example.template.domain.post.domain;

import java.util.List;
import java.util.Optional;

/** 게시글 저장소 포트. */
public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(Long id);

    List<Post> findAllByIdIn(List<Long> ids);

    long countByBoardId(Long boardId);

    /** 조회수를 원자적으로 1 올린다(동시 요청에도 값이 어긋나지 않는다, PRD SCALE-11). */
    void increaseViewCount(Long postId);

    /** 댓글 수를 원자적으로 증감한다. */
    void addCommentCount(Long postId, int delta);
}
