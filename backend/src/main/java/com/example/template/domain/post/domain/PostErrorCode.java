package com.example.template.domain.post.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 게시글 도메인 에러 코드. */
public enum PostErrorCode implements ErrorCode {
    /** 숨김/삭제된 글도 일반 사용자에게는 404로 응답한다(존재 여부를 숨긴다). */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    POST_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "POST_EDIT_FORBIDDEN", "이 글을 수정하거나 삭제할 권한이 없습니다."),
    POST_NOTICE_FORBIDDEN(HttpStatus.FORBIDDEN, "POST_NOTICE_FORBIDDEN", "공지 설정은 관리자만 할 수 있습니다."),
    POST_CONTENT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "POST_CONTENT_TOO_LARGE", "본문이 너무 깁니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PostErrorCode(HttpStatus status, String code, String message) {
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
