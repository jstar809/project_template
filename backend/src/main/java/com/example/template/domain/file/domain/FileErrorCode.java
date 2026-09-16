package com.example.template.domain.file.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 파일 도메인 에러 코드. */
public enum FileErrorCode implements ErrorCode {
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "빈 파일은 업로드할 수 없습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "파일 크기가 허용 범위를 넘었습니다."),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FILE_TYPE_NOT_ALLOWED", "허용되지 않은 파일 형식입니다."),
    FILE_ATTACHMENT_DISABLED(HttpStatus.FORBIDDEN, "FILE_ATTACHMENT_DISABLED", "이 게시판은 첨부를 허용하지 않습니다."),
    FILE_NOT_OWNED(HttpStatus.FORBIDDEN, "FILE_NOT_OWNED", "본인이 업로드한 파일이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    FileErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
