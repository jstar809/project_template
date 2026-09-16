package com.example.template.domain.board.domain;

import java.util.List;
import java.util.Optional;

/** 게시판 저장소 포트. */
public interface BoardRepository {

    Board save(Board board);

    Optional<Board> findById(Long id);

    Optional<Board> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Board> findAllByIdIn(List<Long> ids);

    /** 관리자 화면용: 삭제되지 않은 전체 목록 */
    List<Board> findAllActiveOrNot();
}
