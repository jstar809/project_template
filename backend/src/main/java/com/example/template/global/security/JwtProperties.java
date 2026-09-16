package com.example.template.global.security;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정. 값은 환경변수로 주입한다.
 *
 * @param secret 서명 키. HS256이므로 최소 32바이트가 필요하다.
 * @param accessTtl Access Token 유효 기간 (예: PT15M)
 * @param refreshTtl Refresh Token 유효 기간 (예: P14D)
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(@NotBlank String secret, Duration accessTtl, Duration refreshTtl) {

    public JwtProperties {
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(14) : refreshTtl;
    }
}
