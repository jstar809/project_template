package com.example.template.global.ratelimit;

import com.example.template.global.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bucket4j 기반 인메모리 rate limiter.
 *
 * <p>기본값은 5분에 {@code app.rate-limit.login}회(기본 10회)다. 토큰 버킷 방식이라 시간이 지나면 다시 채워진다.
 *
 * <p>한계: 인스턴스마다 따로 센다. 서버가 2대면 실질 한도가 2배가 된다(PRD SCALE-05). 엄격한 제한이 필요하면 Redis 구현으로 교체한다.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;

    public InMemoryRateLimiter(AppProperties appProperties) {
        this.capacity = appProperties.rateLimit().login();
    }

    @Override
    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
    }

    @Override
    public void reset(String key) {
        buckets.remove(key);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, WINDOW)
                        .build())
                .build();
    }
}
