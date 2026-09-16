package com.example.template.domain.auth.api;

import com.example.template.domain.auth.application.AuthResult;
import com.example.template.domain.auth.application.AuthService;
import com.example.template.domain.auth.domain.AuthErrorCode;
import com.example.template.domain.user.api.UserResponse;
import com.example.template.domain.user.domain.User;
import com.example.template.global.config.AppProperties;
import com.example.template.global.error.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API.
 *
 * <p>Access Token은 응답 바디로, Refresh Token은 httpOnly 쿠키로 내려준다. 쿠키는 자바스크립트가 읽을 수 없어 XSS로 탈취하기 어렵다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")
@SecurityRequirements // 이 컨트롤러는 인증 없이 호출한다
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        User user = authService.signup(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "Access Token과 Refresh Token 쿠키를 발급한다.")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResult result =
                authService.login(request.email(), request.password(), clientIp(httpRequest), userAgent(httpRequest));
        return tokenResponse(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token으로 새 토큰을 발급하고 기존 토큰은 폐기한다.")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        validateOrigin(httpRequest);
        AuthResult result = authService.refresh(refreshToken, clientIp(httpRequest), userAgent(httpRequest));
        return tokenResponse(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 폐기하고 쿠키를 지운다.")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest httpRequest) {
        validateOrigin(httpRequest);
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(AuthResult result) {
        ResponseCookie cookie = refreshCookie(result.refreshToken());
        TokenResponse body =
                new TokenResponse(result.accessToken(), result.expiresInSeconds(), UserResponse.from(result.user()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .sameSite(appProperties.cookie().sameSite())
                .path(COOKIE_PATH)
                .maxAge(authService.refreshTtlSeconds())
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .sameSite(appProperties.cookie().sameSite())
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    /**
     * 쿠키로 인증하는 엔드포인트는 Origin 헤더를 검사한다.
     *
     * <p>SameSite=Strict 쿠키에 더한 2차 방어다(PRD NFR-SEC-02). Origin이 없는 요청(서버 간 호출, 일부 브라우저)은 통과시킨다.
     */
    private void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }
        boolean allowed = appProperties.cors().allowedOrigins().stream().anyMatch(origin::equalsIgnoreCase);
        if (!allowed) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
