package com.example.template.domain.post.application;

import com.example.template.global.response.PageResponse;
import java.util.List;

/**
 * 목록 조회 결과.
 *
 * @param notices 상단 고정 공지 (첫 페이지에서만 채워진다)
 * @param page 일반 글 페이지
 */
public record PostListResult(List<PostListItem> notices, PageResponse<PostListItem> page) {}
