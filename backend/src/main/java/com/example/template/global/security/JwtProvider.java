package com.example.template.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Access Token(JWT) 발급과 검증.
 *
 * <p>Refresh Token은 JWT가 아니라 불투명한 랜덤 문자열이다. 서버가 DB에서 폐기 여부를 관리해야 하기 때문이다(JWT는 한 번 발급하면 만료 전에는
 * 취소할 수 없다).
 */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtProvider(JwtProperties properties, Clock clock) {
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 은 32바이트 이상이어야 합니다. 환경변수 JWT_SECRET 을 확인하세요.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.properties = properties;
        this.clock = clock;
    }

    /** Access Token을 발급한다. */
    public String createAccessToken(Long userId, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTtl());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /** 토큰이 유효하면 사용자 정보를 돌려준다. 위조·만료된 토큰이면 비어 있다. */
    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            return Optional.of(new AuthPrincipal(userId, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long accessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }
}
