package com.example.template.domain.user.domain;

/** 계정 상태. SUSPENDED 계정은 로그인과 토큰 재발급이 막힌다. */
public enum UserStatus {
    ACTIVE,
    SUSPENDED
}
