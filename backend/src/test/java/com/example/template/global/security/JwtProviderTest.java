package com.example.template.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** JWT 발급/검증 단위 테스트. 시간을 고정해 만료를 검증한다. */
@DisplayName("JwtProvider")
class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-unit-test-32bytes-min!";
    private static final Instant NOW = Instant.parse("2026-09-16T00:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(14));

    @Test
    @DisplayName("발급한 토큰에서 사용자 id와 역할을 읽는다")
    void createAndParse() {
        JwtProvider provider = new JwtProvider(properties, fixedClock(NOW));

        String token = provider.createAccessToken(42L, "ADMIN");
        AuthPrincipal principal = provider.parse(token).orElseThrow();

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 읽히지 않는다")
    void expiredToken() {
        String token = new JwtProvider(properties, fixedClock(NOW)).createAccessToken(1L, "USER");

        // 16분 뒤 검증 (유효기간 15분)
        JwtProvider later = new JwtProvider(properties, fixedClock(NOW.plus(Duration.ofMinutes(16))));

        assertThat(later.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 읽히지 않는다")
    void tamperedToken() {
        String token = new JwtProvider(properties, fixedClock(NOW)).createAccessToken(1L, "USER");

        JwtProperties otherKey =
                new JwtProperties("another-secret-key-32bytes-minimum-length!", Duration.ofMinutes(15), null);
        JwtProvider other = new JwtProvider(otherKey, fixedClock(NOW));

        assertThat(other.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("비밀 키가 32바이트 미만이면 기동에 실패한다")
    void shortSecretRejected() {
        JwtProperties shortSecret = new JwtProperties("too-short", null, null);

        assertThatThrownBy(() -> new JwtProvider(shortSecret, fixedClock(NOW)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
