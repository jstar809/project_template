package com.example.template.domain.board.infra.jpa;

import com.example.template.domain.board.domain.Board;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 게시판 JPA 저장소. */
public interface BoardJpaRepository extends JpaRepository<Board, Long> {

    Optional<Board> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    List<Board> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("select b from Board b where b.deletedAt is null order by b.sortOrder asc, b.id asc")
    List<Board> findAllNotDeleted();

    Optional<Board> findByIdAndDeletedAtIsNull(Long id);
}
