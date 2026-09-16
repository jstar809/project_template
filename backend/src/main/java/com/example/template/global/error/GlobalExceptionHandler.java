package com.example.template.global.error;

import com.example.template.global.logging.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 RFC 9457 ProblemDetail 형식으로 변환한다.
 *
 * <p>응답에는 표준 필드에 더해 {@code code}(기계용 에러 코드), {@code traceId}(로그 추적용), 검증 실패 시 {@code
 * errors} 배열이 들어간다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 필드 검증 실패 한 건. */
    public record FieldErrorDetail(String field, String reason) {}

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        log.warn("business error: code={} message={}", code.getCode(), e.getMessage());
        return problem(code.getStatus(), code.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toDetail)
                .toList();
        ProblemDetail problem = problem(
                CommonErrorCode.INVALID_INPUT.getStatus(),
                CommonErrorCode.INVALID_INPUT.getCode(),
                CommonErrorCode.INVALID_INPUT.getMessage(),
                request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        List<FieldErrorDetail> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        ProblemDetail problem = problem(
                CommonErrorCode.INVALID_INPUT.getStatus(),
                CommonErrorCode.INVALID_INPUT.getCode(),
                CommonErrorCode.INVALID_INPUT.getMessage(),
                request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("bad request: {}", e.getMessage());
        return problem(
                CommonErrorCode.INVALID_INPUT.getStatus(),
                CommonErrorCode.INVALID_INPUT.getCode(),
                CommonErrorCode.INVALID_INPUT.getMessage(),
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e, HttpServletRequest request) {
        return problem(
                CommonErrorCode.UNAUTHORIZED.getStatus(),
                CommonErrorCode.UNAUTHORIZED.getCode(),
                CommonErrorCode.UNAUTHORIZED.getMessage(),
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return problem(
                CommonErrorCode.FORBIDDEN.getStatus(),
                CommonErrorCode.FORBIDDEN.getCode(),
                CommonErrorCode.FORBIDDEN.getMessage(),
                request);
    }

    /**
     * 매핑되지 않은 경로 요청.
     *
     * <p>Spring Boot 4에서는 정적 리소스 매핑을 끄면 {@link NoHandlerFoundException}이, 정적 리소스 경로에서는 {@link
     * NoResourceFoundException}이 발생한다. 둘 다 404로 처리한다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNoResource(Exception e, HttpServletRequest request) {
        return problem(
                CommonErrorCode.NOT_FOUND.getStatus(),
                CommonErrorCode.NOT_FOUND.getCode(),
                CommonErrorCode.NOT_FOUND.getMessage(),
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return problem(
                CommonErrorCode.METHOD_NOT_ALLOWED.getStatus(),
                CommonErrorCode.METHOD_NOT_ALLOWED.getCode(),
                CommonErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return problem(
                CommonErrorCode.PAYLOAD_TOO_LARGE.getStatus(),
                CommonErrorCode.PAYLOAD_TOO_LARGE.getCode(),
                CommonErrorCode.PAYLOAD_TOO_LARGE.getMessage(),
                request);
    }

    /** 처리하지 못한 예외. 내부 메시지는 응답에 노출하지 않고 로그에만 남긴다. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("unexpected error", e);
        return problem(
                CommonErrorCode.INTERNAL_ERROR.getStatus(),
                CommonErrorCode.INTERNAL_ERROR.getCode(),
                CommonErrorCode.INTERNAL_ERROR.getMessage(),
                request);
    }

    private static FieldErrorDetail toDetail(FieldError fieldError) {
        String reason = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "올바르지 않은 값입니다.";
        return new FieldErrorDetail(fieldError.getField(), reason);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }
}
