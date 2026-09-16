package com.example.template.global.error;

/**
 * 업무 규칙 위반을 나타내는 예외.
 *
 * <p>서비스 계층에서 {@code throw new BusinessException(PostErrorCode.POST_NOT_FOUND)} 형태로 던지면
 * {@link GlobalExceptionHandler}가 ErrorCode의 상태와 코드로 응답을 만든다.
 */
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 쓰고 싶을 때 사용한다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
