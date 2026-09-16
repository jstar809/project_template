package com.example.template.domain.file.api;

import com.example.template.domain.file.application.FileService;
import com.example.template.domain.file.domain.FileErrorCode;
import com.example.template.domain.file.domain.FileStatus;
import com.example.template.domain.file.domain.StoredFile;
import com.example.template.global.error.BusinessException;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 파일 업로드/다운로드 API. */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File", description = "파일 API")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    public record FileResponse(Long id, String url, String originalName, String contentType, long size) {

        static FileResponse from(StoredFile file) {
            return new FileResponse(
                    file.getId(),
                    "/api/v1/files/" + file.getId(),
                    file.getOriginalName(),
                    file.getContentType(),
                    file.getSizeBytes());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "업로드 직후에는 임시 상태이며, 게시글에 연결되면 보관된다.")
    public ResponseEntity<FileResponse> upload(
            @RequestPart("file") MultipartFile file, @AuthenticationPrincipal AuthPrincipal principal) {
        StoredFile stored = fileService.upload(file, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(FileResponse.from(stored));
    }

    @GetMapping("/{id}")
    @Operation(summary = "파일 다운로드", description = "임시 파일은 업로더만, 연결된 파일은 게시글을 볼 수 있는 사용자만 받을 수 있다.")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        StoredFile file = fileService.getById(id);

        // 아직 게시글에 연결되지 않은 파일은 올린 사람만 볼 수 있다.
        if (file.getStatus() == FileStatus.TEMP && (principal == null || !file.isUploader(principal.userId()))) {
            throw new BusinessException(FileErrorCode.FILE_NOT_OWNED);
        }

        InputStream content = fileService.read(file);
        String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = (file.isImage() ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.getSizeBytes())
                .body(new InputStreamResource(content));
    }
}
