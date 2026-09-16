package com.example.template.global.ratelimit;

/**
 * 호출 횟수 제한 추상화. 주로 로그인 시도 제한에 쓴다.
 *
 * <p>기본 구현은 인스턴스별 인메모리라, 서버를 여러 대 띄우면 인스턴스마다 따로 센다(PRD SCALE-05). 엄격한 제한이 필요하면 Redis 기반 구현으로 교체한다.
 */
public interface RateLimiter {

    /**
     * 키 기준으로 한 번 시도한다.
     *
     * @return 허용되면 true, 한도를 초과했으면 false
     */
    boolean tryConsume(String key);

    /** 로그인 성공처럼 카운트를 초기화해야 할 때 호출한다. */
    void reset(String key);
}
