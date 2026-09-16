package com.example.template.global.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bucket4j 기반 인메모리 rate limiter.
 *
 * <p>기본값은 5분에 {@code LOGIN_RATE_LIMIT}회(기본 10회)다. 토큰 버킷 방식이라 시간이 지나면 다시 채워진다.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;

    public InMemoryRateLimiter(@Value("${app.rate-limit.login:10}") long capacity) {
        this.capacity = capacity;
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
