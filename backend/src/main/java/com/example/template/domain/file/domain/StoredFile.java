package com.example.template.domain.file.domain;

import com.example.template.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 업로드된 파일의 메타데이터. 실제 바이트는 StorageService가 보관한다. */
@Entity
@Table(name = "files")
public class StoredFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 저장소 안에서의 경로. 예: 2026/09/16/uuid.png */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    protected StoredFile() {}

    private StoredFile(String storageKey, String originalName, String contentType, long sizeBytes, Long uploaderId) {
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploaderId = uploaderId;
        this.status = FileStatus.TEMP;
    }

    public static StoredFile upload(
            String storageKey, String originalName, String contentType, long sizeBytes, Long uploaderId) {
        return new StoredFile(storageKey, originalName, contentType, sizeBytes, uploaderId);
    }

    public void link() {
        this.status = FileStatus.LINKED;
    }

    public void unlink() {
        this.status = FileStatus.TEMP;
    }

    public boolean isUploader(Long userId) {
        return userId != null && uploaderId.equals(userId);
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public Long getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public FileStatus getStatus() {
        return status;
    }
}
