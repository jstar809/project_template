package com.example.template.domain.auth.infra.jpa;

import com.example.template.domain.auth.domain.RefreshToken;
import com.example.template.domain.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** domain 포트를 JPA 구현과 연결한다. */
@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash);
    }

    @Override
    public int revokeAllByUserId(Long userId, Instant now) {
        return jpaRepository.revokeAllByUserId(userId, now);
    }

    @Override
    public int deleteExpiredBefore(Instant threshold) {
        return jpaRepository.deleteExpiredBefore(threshold);
    }
}
