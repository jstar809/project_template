package com.example.template.domain.user.api;

import com.example.template.domain.user.domain.User;
import java.time.Instant;

/** 사용자 응답 DTO. Entity를 그대로 노출하지 않는다(PRD ARCH-02). */
public record UserResponse(Long id, String email, String nickname, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
