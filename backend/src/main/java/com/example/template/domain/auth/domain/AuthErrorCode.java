package com.example.template.domain.auth.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 인증 도메인 에러 코드. */
public enum AuthErrorCode implements ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_SUSPENDED", "정지된 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN", "다시 로그인해 주세요."),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH_TOO_MANY_ATTEMPTS", "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
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
