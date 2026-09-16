package com.example.template.global.response;

/**
 * 목록 API 공통 페이징 파라미터.
 *
 * <p>size 상한을 두어 한 번에 너무 많은 데이터를 조회하지 못하게 막는다(PRD SCALE-09).
 */
public record PageRequestParams(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequestParams {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public static PageRequestParams of(Integer page, Integer size) {
        return new PageRequestParams(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    /** MyBatis 쿼리에 넘길 OFFSET 값. */
    public int offset() {
        return page * size;
    }
}
