package com.example.template.domain.file.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 파일 저장소 포트. */
public interface FileRepository {

    StoredFile save(StoredFile file);

    Optional<StoredFile> findById(Long id);

    List<StoredFile> findAllByIdIn(List<Long> ids);

    void delete(StoredFile file);

    /** 게시글에 연결된 파일들 */
    List<PostFile> findLinksByPostId(Long postId);

    PostFile saveLink(PostFile postFile);

    void deleteLinksByPostId(Long postId);

    /** 오래된 임시 파일 정리 대상 */
    List<StoredFile> findTempFilesUploadedBefore(Instant threshold, int limit);
}
