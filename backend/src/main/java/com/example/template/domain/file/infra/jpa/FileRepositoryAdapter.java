package com.example.template.domain.file.infra.jpa;

import com.example.template.domain.file.domain.FileRepository;
import com.example.template.domain.file.domain.FileStatus;
import com.example.template.domain.file.domain.PostFile;
import com.example.template.domain.file.domain.StoredFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

/** 파일 포트를 JPA 구현과 연결한다. */
@Repository
public class FileRepositoryAdapter implements FileRepository {

    private final FileJpaRepository fileJpaRepository;
    private final PostFileJpaRepository postFileJpaRepository;

    public FileRepositoryAdapter(FileJpaRepository fileJpaRepository, PostFileJpaRepository postFileJpaRepository) {
        this.fileJpaRepository = fileJpaRepository;
        this.postFileJpaRepository = postFileJpaRepository;
    }

    @Override
    public StoredFile save(StoredFile file) {
        return fileJpaRepository.save(file);
    }

    @Override
    public Optional<StoredFile> findById(Long id) {
        return fileJpaRepository.findById(id);
    }

    @Override
    public List<StoredFile> findAllByIdIn(List<Long> ids) {
        return fileJpaRepository.findAllById(ids);
    }

    @Override
    public void delete(StoredFile file) {
        fileJpaRepository.delete(file);
    }

    @Override
    public List<PostFile> findLinksByPostId(Long postId) {
        return postFileJpaRepository.findAllByIdPostIdOrderBySortOrderAsc(postId);
    }

    @Override
    public PostFile saveLink(PostFile postFile) {
        return postFileJpaRepository.save(postFile);
    }

    @Override
    public void deleteLinksByPostId(Long postId) {
        postFileJpaRepository.deleteAllByIdPostId(postId);
    }

    @Override
    public List<StoredFile> findTempFilesUploadedBefore(Instant threshold, int limit) {
        return fileJpaRepository.findAllByStatusAndCreatedAtBefore(FileStatus.TEMP, threshold, Limit.of(limit));
    }
}
