package com.example.template.domain.post.application;

/**
 * 게시글 검색 조건.
 *
 * @param boardId 대상 게시판
 * @param searchType title / content / title_content / author (null이면 검색 없음)
 * @param keyword 검색어
 * @param offset 건너뛸 개수
 * @param limit 가져올 개수
 */
public record PostSearchCondition(Long boardId, String searchType, String keyword, int offset, int limit) {

    public PostSearchCondition {
        // 화이트리스트 밖의 값은 검색 없음으로 취급한다(SQL Injection 및 오타 방지).
        if (searchType != null
                && !searchType.equals("title")
                && !searchType.equals("content")
                && !searchType.equals("title_content")
                && !searchType.equals("author")) {
            searchType = null;
        }
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
            searchType = null;
        }
        if (keyword == null) {
            searchType = null;
        }
    }

    public boolean hasKeyword() {
        return keyword != null && searchType != null;
    }
}
