package com.example.template.domain.board.domain;

import com.example.template.domain.user.domain.Role;

/**
 * 게시판 접근 권한 단계.
 *
 * <p>ANONYMOUS &lt; USER &lt; ADMIN 순서이며, 사용자의 단계가 요구 단계 이상이면 허용한다(PRD FR-BOARD-06).
 */
public enum Permission {
    ANONYMOUS(0),
    USER(1),
    ADMIN(2);

    private final int level;

    Permission(int level) {
        this.level = level;
    }

    /** 로그인하지 않은 사용자는 role이 null이다. */
    public boolean allows(Role role) {
        return levelOf(role) >= this.level;
    }

    private static int levelOf(Role role) {
        if (role == null) {
            return ANONYMOUS.level;
        }
        return role == Role.ADMIN ? ADMIN.level : USER.level;
    }
}
