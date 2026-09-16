package com.example.template.domain.auth.api;

import com.example.template.domain.user.api.UserResponse;

/**
 * 로그인/재발급 응답.
 *
 * @param accessToken 클라이언트는 이 값을 메모리에만 보관한다(localStorage 금지)
 * @param expiresIn 만료까지 남은 초
 */
public record TokenResponse(String accessToken, long expiresIn, UserResponse user) {}
