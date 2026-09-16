package com.example.template.domain.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.template.domain.file.application.FileValidator;
import com.example.template.global.error.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 업로드 파일 검증 단위 테스트 (PRD FR-FILE-02). */
@DisplayName("파일 검증")
class FileValidatorTest {

    private static final long MAX = 10 * 1024 * 1024;

    /** PNG 파일의 시작 바이트(매직 넘버) */
    private static final byte[] PNG_HEADER = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'
    };

    /** Windows 실행 파일의 시작 바이트 */
    private static final byte[] EXE_HEADER = {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};

    private final FileValidator validator = new FileValidator();

    @Test
    @DisplayName("확장자와 실제 형식이 맞으면 통과한다")
    void allowsValidPng() {
        String contentType = validator.validate("photo.png", PNG_HEADER.length, MAX, stream(PNG_HEADER));

        assertThat(contentType).isEqualTo("image/png");
    }

    @Test
    @DisplayName("실행 파일을 png로 위장하면 거부한다")
    void rejectsDisguisedExecutable() {
        assertThatThrownBy(() -> validator.validate("virus.png", EXE_HEADER.length, MAX, stream(EXE_HEADER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일치하지 않습니다");
    }

    @Test
    @DisplayName("허용하지 않는 확장자는 거부한다")
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() -> validator.validate("script.svg", 100, MAX, stream(PNG_HEADER)))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> validator.validate("run.exe", 100, MAX, stream(EXE_HEADER)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 파일과 크기 초과 파일을 거부한다")
    void rejectsEmptyAndTooLarge() {
        assertThatThrownBy(() -> validator.validate("photo.png", 0, MAX, stream(PNG_HEADER)))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> validator.validate("photo.png", MAX + 1, MAX, stream(PNG_HEADER)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("확장자가 없으면 거부한다")
    void rejectsNoExtension() {
        assertThatThrownBy(() -> validator.validate("noextension", 100, MAX, stream(PNG_HEADER)))
                .isInstanceOf(BusinessException.class);
    }

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }
}
