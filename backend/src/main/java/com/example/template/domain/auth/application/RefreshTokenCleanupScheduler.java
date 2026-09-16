package com.example.template.domain.auth.application;

import java.time.Clock;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 Refresh Token을 주기적으로 지운다.
 *
 * <p>{@link SchedulerLock} 덕분에 인스턴스가 여러 개여도 한 번만 실행된다(PRD SCALE-03).
 */
@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    /** 만료 후에도 이 기간 동안은 남겨둔다(재사용 탐지에 쓰인다). */
    private static final Duration RETENTION = Duration.ofDays(7);

    private final AuthService authService;
    private final Clock clock;

    public RefreshTokenCleanupScheduler(AuthService authService, Clock clock) {
        this.authService = authService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *") // 매일 새벽 3시 30분
    @SchedulerLock(name = "cleanupExpiredRefreshTokens", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void cleanup() {
        int deleted = authService.deleteExpiredTokens(clock.instant().minus(RETENTION));
        if (deleted > 0) {
            log.info("만료된 refresh token {}건을 삭제했다.", deleted);
        }
    }
}
