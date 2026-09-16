package com.example.template.domain.user.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 사용자 도메인 에러 코드. */
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
    USER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    USER_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus status, String code, String message) {
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
