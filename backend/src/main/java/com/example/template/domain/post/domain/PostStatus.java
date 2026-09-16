package com.example.template.domain.post.domain;

/** 게시글 상태. HIDDEN은 관리자가 숨긴 상태로, 일반 사용자에게는 보이지 않는다. */
public enum PostStatus {
    PUBLISHED,
    HIDDEN
}
