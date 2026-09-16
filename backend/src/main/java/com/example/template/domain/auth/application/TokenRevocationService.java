package com.example.template.domain.auth.application;

import com.example.template.domain.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 폐기를 별도 트랜잭션에서 처리한다.
 *
 * <p>왜 필요한가: refresh token 재사용을 감지하면 (1) 해당 사용자의 토큰을 모두 폐기하고 (2) 401 예외를 던져야 한다. 그런데 같은 트랜잭션에서
 * 예외를 던지면 폐기까지 롤백되어 아무 일도 일어나지 않는다. 그래서 폐기는 REQUIRES_NEW로 즉시 커밋한다.
 *
 * <p>self-invocation은 프록시를 타지 않으므로 반드시 별도 빈으로 분리해야 한다.
 */
@Service
public class TokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenRevocationService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllInNewTransaction(Long userId, Instant now) {
        return refreshTokenRepository.revokeAllByUserId(userId, now);
    }
}
