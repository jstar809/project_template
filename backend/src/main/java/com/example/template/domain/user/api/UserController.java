package com.example.template.domain.user.api;

import com.example.template.domain.auth.application.AuthService;
import com.example.template.domain.user.application.UserService;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 정보 관련 API. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    public record UpdateNicknameRequest(
            @NotBlank @Size(min = 2, max = 20) String nickname) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,

            @NotBlank
            @Size(min = 8, max = 64)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
            String newPassword) {}

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return UserResponse.from(userService.getById(principal.userId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "닉네임 변경")
    public UserResponse updateNickname(
            @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody UpdateNicknameRequest request) {
        return UserResponse.from(userService.changeNickname(principal.userId(), request.nickname()));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "비밀번호 변경", description = "변경하면 기존 refresh token이 모두 폐기된다.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        authService.revokeAllTokens(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
