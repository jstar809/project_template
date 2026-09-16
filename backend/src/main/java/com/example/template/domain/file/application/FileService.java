package com.example.template.domain.file.application;

import com.example.template.domain.file.domain.FileErrorCode;
import com.example.template.domain.file.domain.FileRepository;
import com.example.template.domain.file.domain.PostFile;
import com.example.template.domain.file.domain.StoredFile;
import com.example.template.domain.file.domain.UsageType;
import com.example.template.global.config.AppProperties;
import com.example.template.global.error.BusinessException;
import com.example.template.global.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 파일 업로드/다운로드/연결 업무 로직. */
@Service
@Transactional(readOnly = true)
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final DateTimeFormatter KEY_DATE =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final AppProperties appProperties;
    private final Clock clock;

    public FileService(
            FileRepository fileRepository,
            StorageService storageService,
            FileValidator fileValidator,
            AppProperties appProperties,
            Clock clock) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    /** 업로드. 검증을 통과하면 저장소에 쓰고 메타데이터를 TEMP 상태로 남긴다. */
    @Transactional
    public StoredFile upload(MultipartFile multipartFile, Long uploaderId) {
        String originalName = multipartFile.getOriginalFilename();
        long maxBytes = appProperties.storage().maxFileSizeBytes();

        String contentType;
        try (InputStream detectStream = multipartFile.getInputStream()) {
            contentType = fileValidator.validate(originalName, multipartFile.getSize(), maxBytes, detectStream);
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_NOT_ALLOWED, "파일을 읽을 수 없습니다.");
        }

        String key = buildKey(originalName);
        try (InputStream content = multipartFile.getInputStream()) {
            storageService.store(key, content, contentType);
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_NOT_ALLOWED, "파일을 저장하지 못했습니다.");
        }

        return fileRepository.save(
                StoredFile.upload(key, originalName, contentType, multipartFile.getSize(), uploaderId));
    }

    public StoredFile getById(Long fileId) {
        return fileRepository.findById(fileId).orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
    }

    public InputStream read(StoredFile file) {
        return storageService.load(file.getStorageKey());
    }

    /**
     * 게시글에 파일을 연결한다.
     *
     * <p>본인이 올린 TEMP 파일만 연결할 수 있다. 남의 파일 id를 넣어 가져가는 것을 막는다.
     */
    @Transactional
    public void linkToPost(Long postId, Long uploaderId, List<Long> fileIds, List<Long> inlineFileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<StoredFile> files = fileRepository.findAllByIdIn(fileIds);
        if (files.size() != fileIds.size()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }

        Map<Long, StoredFile> byId = files.stream().collect(Collectors.toMap(StoredFile::getId, Function.identity()));
        int order = 0;
        for (Long fileId : fileIds) {
            StoredFile file = byId.get(fileId);
            if (!file.isUploader(uploaderId)) {
                throw new BusinessException(FileErrorCode.FILE_NOT_OWNED);
            }
            file.link();
            UsageType usageType =
                    inlineFileIds != null && inlineFileIds.contains(fileId) ? UsageType.INLINE : UsageType.ATTACHMENT;
            fileRepository.saveLink(PostFile.link(postId, fileId, usageType, order++));
        }
    }

    /** 게시글 수정 시 호출한다. 기존 연결을 끊고 새 목록으로 다시 연결한다. */
    @Transactional
    public void relinkToPost(Long postId, Long uploaderId, List<Long> fileIds, List<Long> inlineFileIds) {
        List<PostFile> existing = fileRepository.findLinksByPostId(postId);
        List<Long> previousIds = existing.stream().map(PostFile::getFileId).toList();

        fileRepository.deleteLinksByPostId(postId);
        // 이번 목록에서 빠진 파일은 TEMP로 되돌려 정리 대상이 되게 한다.
        previousIds.stream()
                .filter(id -> fileIds == null || !fileIds.contains(id))
                .forEach(id -> fileRepository.findById(id).ifPresent(StoredFile::unlink));

        linkToPost(postId, uploaderId, fileIds, inlineFileIds);
    }

    /** 게시글에 연결된 파일 목록 */
    public List<StoredFile> getFilesOfPost(Long postId) {
        List<Long> fileIds = fileRepository.findLinksByPostId(postId).stream()
                .map(PostFile::getFileId)
                .toList();
        return fileIds.isEmpty() ? List.of() : fileRepository.findAllByIdIn(fileIds);
    }

    /** 게시글에 연결된 파일 중 첨부(ATTACHMENT)만 */
    public List<StoredFile> getAttachmentsOfPost(Long postId) {
        List<Long> attachmentIds = fileRepository.findLinksByPostId(postId).stream()
                .filter(link -> link.getUsageType() == UsageType.ATTACHMENT)
                .map(PostFile::getFileId)
                .toList();
        return attachmentIds.isEmpty() ? List.of() : fileRepository.findAllByIdIn(attachmentIds);
    }

    /** 파일이 연결된 게시글 id (다운로드 권한 확인에 쓴다). */
    public List<PostFile> findLinksByPostId(Long postId) {
        return fileRepository.findLinksByPostId(postId);
    }

    /**
     * 오래된 임시 파일을 정리한다. 스케줄러가 호출한다.
     *
     * @return 삭제한 개수
     */
    @Transactional
    public int cleanupTempFiles(Instant threshold, int limit) {
        List<StoredFile> targets = fileRepository.findTempFilesUploadedBefore(threshold, limit);
        List<StoredFile> deleted = new ArrayList<>();
        for (StoredFile file : targets) {
            storageService.delete(file.getStorageKey());
            fileRepository.delete(file);
            deleted.add(file);
        }
        if (!deleted.isEmpty()) {
            log.info("임시 파일 {}건을 정리했다.", deleted.size());
        }
        return deleted.size();
    }

    /** 저장 키: 날짜 폴더 + UUID. 원본 파일명은 DB에만 두어 경로 조작을 원천 차단한다. */
    private String buildKey(String originalName) {
        String extension = FileValidator.extensionOf(originalName);
        String fileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        return KEY_DATE.format(clock.instant()) + "/" + fileName;
    }
}
