package com.example.template.global.security;

/**
 * 인증된 사용자 정보. SecurityContext에 담긴다.
 *
 * <p>DB를 다시 조회하지 않아도 되도록 토큰에 담긴 최소 정보만 갖는다.
 */
public record AuthPrincipal(Long userId, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
