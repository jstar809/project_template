package com.example.template.global.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 설정.
 *
 * <p>서버를 여러 대로 띄우면 같은 스케줄 작업이 동시에 여러 번 실행된다. ShedLock이 DB의 {@code shedlock} 테이블을 잠금 장치로 써서, 한
 * 인스턴스만 작업을 실행하게 한다(PRD SCALE-03).
 *
 * <p>사용 예: {@code @Scheduled(cron = "...") @SchedulerLock(name = "cleanupTempFiles")}
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
                .usingDbTime() // 인스턴스 간 시계 차이를 피하려고 DB 시간을 기준으로 잠근다
                .build());
    }
}
