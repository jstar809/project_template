package com.example.template.global.error;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드 enum이 구현하는 인터페이스.
 *
 * <p>예: {@code PostErrorCode.POST_NOT_FOUND}. 코드 문자열은 클라이언트가 분기에 쓰므로 한 번 정하면 바꾸지 않는다.
 */
public interface ErrorCode {

    /** 응답 HTTP 상태 */
    HttpStatus getStatus();

    /** 기계가 읽는 코드. 예: POST_NOT_FOUND */
    String getCode();

    /** 사람이 읽는 기본 메시지 */
    String getMessage();
}
