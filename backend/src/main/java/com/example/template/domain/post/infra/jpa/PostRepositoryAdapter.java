package com.example.template.domain.post.infra.jpa;

import com.example.template.domain.post.domain.Post;
import com.example.template.domain.post.domain.PostRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 게시글 포트를 JPA 구현과 연결한다. */
@Repository
public class PostRepositoryAdapter implements PostRepository {

    private final PostJpaRepository jpaRepository;

    public PostRepositoryAdapter(PostJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Post save(Post post) {
        return jpaRepository.save(post);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Post> findAllByIdIn(List<Long> ids) {
        return jpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public long countByBoardId(Long boardId) {
        return jpaRepository.countByBoardIdAndDeletedAtIsNull(boardId);
    }

    @Override
    public void increaseViewCount(Long postId) {
        jpaRepository.increaseViewCount(postId);
    }

    @Override
    public void addCommentCount(Long postId, int delta) {
        jpaRepository.addCommentCount(postId, delta);
    }
}
