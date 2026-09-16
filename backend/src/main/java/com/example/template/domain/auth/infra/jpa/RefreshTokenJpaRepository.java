package com.example.template.domain.auth.infra.jpa;

import com.example.template.domain.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refresh Token JPA 저장소. */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 살아 있는 토큰을 한 번의 UPDATE로 모두 폐기한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken t where t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);
}
