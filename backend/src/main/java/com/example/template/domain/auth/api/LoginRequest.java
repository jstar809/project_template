package com.example.template.domain.auth.api;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
