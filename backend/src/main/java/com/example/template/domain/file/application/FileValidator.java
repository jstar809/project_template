package com.example.template.domain.file.application;

import com.example.template.domain.file.domain.FileErrorCode;
import com.example.template.global.error.BusinessException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * 업로드 파일 검증 (PRD FR-FILE-02).
 *
 * <p>확장자만 믿으면 안 된다. 공격자가 실행 파일을 .png로 바꿔 올릴 수 있기 때문이다. 그래서 Tika로 <b>파일 내용의 시그니처(매직 넘버)</b>를 읽어
 * 확장자와 맞는지 확인한다.
 *
 * <p>SVG는 내부에 스크립트를 넣을 수 있어 허용하지 않는다.
 */
@Component
public class FileValidator {

    /** 확장자 → 허용되는 실제 MIME 타입들 */
    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("zip", Set.of("application/zip")),
            Map.entry(
                    "docx",
                    Set.of(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/x-tika-ooxml",
                            "application/zip")),
            Map.entry(
                    "xlsx",
                    Set.of(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/x-tika-ooxml",
                            "application/zip")),
            Map.entry(
                    "pptx",
                    Set.of(
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/x-tika-ooxml",
                            "application/zip")));

    private final Tika tika = new Tika();

    /**
     * 파일을 검증하고 실제 MIME 타입을 돌려준다.
     *
     * @param content mark/reset을 지원해야 한다(내용을 읽은 뒤 되감기 때문)
     */
    public String validate(String originalName, long sizeBytes, long maxBytes, InputStream content) {
        if (sizeBytes <= 0) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }
        if (sizeBytes > maxBytes) {
            throw new BusinessException(FileErrorCode.FILE_TOO_LARGE);
        }

        String extension = extensionOf(originalName);
        Set<String> allowedTypes = ALLOWED.get(extension);
        if (allowedTypes == null) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String detected = detect(content);
        if (!allowedTypes.contains(detected)) {
            throw new BusinessException(
                    FileErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "확장자(%s)와 실제 파일 형식(%s)이 일치하지 않습니다.".formatted(extension, detected));
        }
        return detected;
    }

    public static String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String detect(InputStream content) {
        try {
            BufferedInputStream buffered =
                    content instanceof BufferedInputStream bis ? bis : new BufferedInputStream(content);
            return tika.detect(buffered);
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_NOT_ALLOWED, "파일 형식을 확인할 수 없습니다.");
        }
    }
}
