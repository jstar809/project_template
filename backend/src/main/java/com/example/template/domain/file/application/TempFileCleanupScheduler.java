package com.example.template.domain.file.application;

import java.time.Clock;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 게시글에 연결되지 않은 임시 파일을 정리한다(PRD FR-FILE-06).
 *
 * <p>사용자가 파일만 올리고 글을 저장하지 않으면 파일이 그대로 남는다. 24시간이 지난 TEMP 파일을 지운다.
 *
 * <p>서버가 여러 대여도 ShedLock 덕분에 한 인스턴스에서만 실행된다.
 */
@Component
public class TempFileCleanupScheduler {

    private static final Duration RETENTION = Duration.ofHours(24);
    private static final int BATCH_SIZE = 500;

    private final FileService fileService;
    private final Clock clock;

    public TempFileCleanupScheduler(FileService fileService, Clock clock) {
        this.fileService = fileService;
        this.clock = clock;
    }

    // 기본은 매시 정각. 환경변수 TEMP_FILE_CLEANUP_CRON 으로 바꿀 수 있다(운영 점검·테스트용).
    @Scheduled(cron = "${app.scheduler.temp-file-cleanup-cron:0 0 * * * *}")
    @SchedulerLock(name = "cleanupTempFiles", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void cleanup() {
        fileService.cleanupTempFiles(clock.instant().minus(RETENTION), BATCH_SIZE);
    }
}
