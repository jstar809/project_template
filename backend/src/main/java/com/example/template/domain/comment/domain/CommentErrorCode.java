package com.example.template.domain.comment.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 댓글 도메인 에러 코드. */
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    COMMENT_DISABLED(HttpStatus.FORBIDDEN, "COMMENT_DISABLED", "이 게시판은 댓글을 허용하지 않습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT_DEPTH_EXCEEDED", "대댓글에는 답글을 달 수 없습니다."),
    COMMENT_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_EDIT_FORBIDDEN", "이 댓글을 수정하거나 삭제할 권한이 없습니다."),
    COMMENT_PARENT_MISMATCH(HttpStatus.BAD_REQUEST, "COMMENT_PARENT_MISMATCH", "다른 글의 댓글에는 답글을 달 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommentErrorCode(HttpStatus status, String code, String message) {
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
