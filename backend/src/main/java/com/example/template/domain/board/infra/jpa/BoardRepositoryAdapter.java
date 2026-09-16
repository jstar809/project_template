package com.example.template.domain.board.infra.jpa;

import com.example.template.domain.board.domain.Board;
import com.example.template.domain.board.domain.BoardRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 게시판 포트를 JPA 구현과 연결한다. */
@Repository
public class BoardRepositoryAdapter implements BoardRepository {

    private final BoardJpaRepository jpaRepository;

    public BoardRepositoryAdapter(BoardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Board save(Board board) {
        return jpaRepository.save(board);
    }

    @Override
    public Optional<Board> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Board> findBySlug(String slug) {
        return jpaRepository.findBySlugAndDeletedAtIsNull(Board.normalizeSlug(slug));
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlugAndDeletedAtIsNull(Board.normalizeSlug(slug));
    }

    @Override
    public List<Board> findAllByIdIn(List<Long> ids) {
        return jpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public List<Board> findAllActiveOrNot() {
        return jpaRepository.findAllNotDeleted();
    }
}
