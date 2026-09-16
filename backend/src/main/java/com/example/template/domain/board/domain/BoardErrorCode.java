package com.example.template.domain.board.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 게시판 도메인 에러 코드. */
public enum BoardErrorCode implements ErrorCode {
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD_NOT_FOUND", "게시판을 찾을 수 없습니다."),
    BOARD_SLUG_DUPLICATED(HttpStatus.CONFLICT, "BOARD_SLUG_DUPLICATED", "이미 사용 중인 게시판 주소입니다."),
    BOARD_HAS_POSTS(HttpStatus.CONFLICT, "BOARD_HAS_POSTS", "게시글이 있는 게시판은 삭제할 수 없습니다. 비활성화를 사용하세요."),
    BOARD_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "BOARD_READ_FORBIDDEN", "이 게시판을 볼 권한이 없습니다."),
    BOARD_WRITE_FORBIDDEN(HttpStatus.FORBIDDEN, "BOARD_WRITE_FORBIDDEN", "이 게시판에 글을 쓸 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BoardErrorCode(HttpStatus status, String code, String message) {
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
