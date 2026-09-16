package com.example.template.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정.
 *
 * <p>Spring Cache 추상화를 쓰므로, 나중에 Redis로 바꿔도 서비스 코드는 그대로다(PRD SCALE-04). 지금은 인스턴스별 로컬 캐시(Caffeine)다.
 * 여러 인스턴스를 띄우면 인스턴스마다 캐시를 따로 갖기 때문에, 자주 바뀌지 않는 데이터(게시판 목록 등)만 캐시한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 게시판 목록 캐시 이름. M4에서 사용한다. */
    public static final String BOARDS = "boards";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(BOARDS);
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(1_000).expireAfterWrite(Duration.ofMinutes(10)));
        return manager;
    }
}
