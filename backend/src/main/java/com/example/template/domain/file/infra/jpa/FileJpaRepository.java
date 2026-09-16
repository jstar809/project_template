package com.example.template.domain.file.infra.jpa;

import com.example.template.domain.file.domain.FileStatus;
import com.example.template.domain.file.domain.StoredFile;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** 파일 메타데이터 JPA 저장소. */
public interface FileJpaRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findAllByStatusAndCreatedAtBefore(FileStatus status, Instant threshold, Limit limit);
}
