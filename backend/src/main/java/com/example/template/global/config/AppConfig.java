package com.example.template.global.config;

import com.example.template.global.security.JwtProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 애플리케이션 전역에서 쓰는 단순 빈 모음. */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
public class AppConfig {

    /**
     * 시간을 빈으로 주입받으면 테스트에서 시간을 고정할 수 있다(PRD ARCH-07).
     *
     * <p>모든 시각은 UTC로 다룬다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
