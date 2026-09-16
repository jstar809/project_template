package com.example.template.global.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 응답 공통 형식. page는 0부터 시작한다.
 *
 * @param content 현재 페이지 데이터
 * @param page 현재 페이지 번호(0-based)
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 * @param hasNext 다음 페이지 존재 여부
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext);
    }

    /** Spring Data의 Page를 그대로 변환할 때 사용한다. */
    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }

    /** content의 타입만 바꾼 새 응답을 만든다. */
    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
                content.stream().map(mapper).toList(), page, size, totalElements, totalPages, hasNext);
    }
}
