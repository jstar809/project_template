package com.example.template.domain.auth.application;

import com.example.template.domain.user.domain.User;

/**
 * 로그인/재발급 결과.
 *
 * @param accessToken 클라이언트 메모리에 보관할 Access Token
 * @param expiresInSeconds Access Token 만료까지 남은 초
 * @param refreshToken 쿠키로 내려줄 원본 Refresh Token (DB에는 해시만 저장된다)
 * @param user 로그인한 사용자
 */
public record AuthResult(String accessToken, long expiresInSeconds, String refreshToken, User user) {}
