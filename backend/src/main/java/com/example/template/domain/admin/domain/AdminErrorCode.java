package com.example.template.domain.admin.domain;

import com.example.template.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 관리자 기능 에러 코드. */
public enum AdminErrorCode implements ErrorCode {
    /** 관리자가 자기 자신의 역할을 바꾸거나 계정을 정지하면 시스템에 관리자가 없어질 수 있다. */
    ADMIN_SELF_MODIFICATION(HttpStatus.BAD_REQUEST, "ADMIN_SELF_MODIFICATION", "자기 자신의 역할이나 상태는 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AdminErrorCode(HttpStatus status, String code, String message) {
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
