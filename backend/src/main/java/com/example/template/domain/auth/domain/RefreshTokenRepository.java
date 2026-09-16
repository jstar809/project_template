package com.example.template.domain.auth.domain;

import java.time.Instant;
import java.util.Optional;

/** Refresh Token 저장소 포트. */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 해당 사용자의 살아 있는 토큰을 모두 폐기한다. 재사용 탐지와 비밀번호 변경 시 사용한다. */
    int revokeAllByUserId(Long userId, Instant now);

    /** 만료된 지 오래된 토큰을 정리한다. */
    int deleteExpiredBefore(Instant threshold);
}
