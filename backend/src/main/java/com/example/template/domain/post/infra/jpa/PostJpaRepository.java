package com.example.template.domain.post.infra.jpa;

import com.example.template.domain.post.domain.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 게시글 JPA 저장소. */
public interface PostJpaRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    List<Post> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    long countByBoardIdAndDeletedAtIsNull(Long boardId);

    /** UPDATE ... SET x = x + 1 형태라 동시에 호출되어도 값이 정확하다. */
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long id);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + :delta where p.id = :id")
    void addCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
