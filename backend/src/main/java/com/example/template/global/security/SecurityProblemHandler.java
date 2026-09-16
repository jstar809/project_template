package com.example.template.global.security;

import com.example.template.global.error.CommonErrorCode;
import com.example.template.global.error.ErrorCode;
import com.example.template.global.logging.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증 실패(401)와 권한 부족(403) 응답도 다른 에러와 같은 ProblemDetail 형식으로 돌려준다.
 *
 * <p>Security 필터에서 발생하는 예외는 @RestControllerAdvice까지 오지 않기 때문에 여기서 직접 만든다.
 */
@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityProblemHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        write(request, response, CommonErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        write(request, response, CommonErrorCode.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
        problem.setTitle(errorCode.getStatus().getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.getCode());
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
