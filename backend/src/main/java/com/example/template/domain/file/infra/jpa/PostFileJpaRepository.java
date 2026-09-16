package com.example.template.domain.file.infra.jpa;

import com.example.template.domain.file.domain.PostFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글-파일 연결 JPA 저장소. */
public interface PostFileJpaRepository extends JpaRepository<PostFile, PostFile.PostFileId> {

    List<PostFile> findAllByIdPostIdOrderBySortOrderAsc(Long postId);

    void deleteAllByIdPostId(Long postId);
}
