package com.example.template.domain.auth.application;

import com.example.template.domain.auth.domain.AuthErrorCode;
import com.example.template.domain.auth.domain.RefreshToken;
import com.example.template.domain.auth.domain.RefreshTokenRepository;
import com.example.template.domain.user.application.UserService;
import com.example.template.domain.user.domain.User;
import com.example.template.global.error.BusinessException;
import com.example.template.global.ratelimit.RateLimiter;
import com.example.template.global.security.JwtProperties;
import com.example.template.global.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 업무 로직: 회원가입, 로그인, 토큰 재발급, 로그아웃.
 *
 * <p>Refresh Token은 재발급할 때마다 새 토큰으로 교체한다(rotation). 이미 폐기된 토큰이 다시 들어오면 토큰이 탈취되었다고 보고 그 사용자의 토큰을 모두
 * 폐기한다(재사용 탐지, PRD FR-AUTH-05).
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService tokenRevocationService;
    private final JwtProvider jwtProvider;
    private final RateLimiter rateLimiter;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthService(
            UserService userService,
            RefreshTokenRepository refreshTokenRepository,
            TokenRevocationService tokenRevocationService,
            JwtProvider jwtProvider,
            RateLimiter rateLimiter,
            JwtProperties jwtProperties,
            Clock clock) {
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenRevocationService = tokenRevocationService;
        this.jwtProvider = jwtProvider;
        this.rateLimiter = rateLimiter;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public User signup(String email, String password, String nickname) {
        return userService.register(email, password, nickname);
    }

    /**
     * 로그인.
     *
     * @param clientIp 호출 제한 키로 쓴다
     */
    @Transactional
    public AuthResult login(String email, String password, String clientIp, String userAgent) {
        String rateLimitKey = "login:" + clientIp + ":" + User.normalizeEmail(email);
        if (!rateLimiter.tryConsume(rateLimitKey)) {
            throw new BusinessException(AuthErrorCode.TOO_MANY_ATTEMPTS);
        }

        User user = userService
                .findByEmail(email)
                // 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해서 알려주지 않는다(계정 존재 여부 노출 방지)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!userService.matchesPassword(user, password)) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
        }

        rateLimiter.reset(rateLimitKey);
        userService.recordLogin(user);
        return issueTokens(user, clientIp, userAgent);
    }

    /** Refresh Token으로 새 토큰을 발급한다(rotation + 재사용 탐지). */
    @Transactional
    public AuthResult refresh(String rawRefreshToken, String clientIp, String userAgent) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isRevoked()) {
            // 이미 폐기된 토큰이 다시 쓰였다 = 탈취 가능성. 해당 사용자의 모든 토큰을 끊는다.
            // 아래에서 예외를 던지므로, 폐기가 롤백되지 않도록 별도 트랜잭션에서 처리한다.
            log.warn("refresh token 재사용 감지: userId={}", stored.getUserId());
            tokenRevocationService.revokeAllInNewTransaction(stored.getUserId(), now);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (stored.isExpired(now)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userService.getById(stored.getUserId());
        if (!user.isActive()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
        }

        stored.revoke(now);
        return issueTokens(user, clientIp, userAgent);
    }

    /** 로그아웃. 전달된 토큰만 폐기한다. */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    /** 비밀번호 변경, 계정 정지 등에서 호출한다. */
    @Transactional
    public int revokeAllTokens(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId, clock.instant());
    }

    /** 만료된 토큰 정리. 스케줄러가 호출한다. */
    @Transactional
    public int deleteExpiredTokens(Instant threshold) {
        return refreshTokenRepository.deleteExpiredBefore(threshold);
    }

    private AuthResult issueTokens(User user, String clientIp, String userAgent) {
        String accessToken =
                jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String rawRefreshToken = generateRefreshToken();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.refreshTtl());

        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), hash(rawRefreshToken), issuedAt, expiresAt, userAgent, clientIp));

        return new AuthResult(accessToken, jwtProvider.accessTtlSeconds(), rawRefreshToken, user);
    }

    /** 추측할 수 없는 랜덤 문자열을 만든다(JWT가 아니다). */
    private static String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** DB에는 원본이 아니라 SHA-256 해시를 저장한다. */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    /** 쿠키 만료 시간을 맞추기 위해 컨트롤러가 사용한다. */
    public long refreshTtlSeconds() {
        return jwtProperties.refreshTtl().toSeconds();
    }
}
