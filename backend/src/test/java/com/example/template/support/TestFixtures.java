package com.example.template.support;

import com.example.template.domain.auth.application.AuthResult;
import com.example.template.domain.auth.application.AuthService;
import com.example.template.domain.user.application.UserService;
import com.example.template.domain.user.domain.Role;
import com.example.template.domain.user.domain.User;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;

/** 테스트에서 사용자/토큰을 빠르게 만드는 헬퍼. */
@TestComponent
public class TestFixtures {

    private static final String PASSWORD = "password123";

    /** 만들어진 계정 정보 */
    public record Account(Long userId, String email, String accessToken) {}

    private final AuthService authService;
    private final UserService userService;

    public TestFixtures(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /** 일반 사용자를 만들고 토큰과 id를 돌려준다. */
    public Account createUser() {
        String email = uniqueEmail();
        User user = userService.register(email, PASSWORD, uniqueNickname());
        return new Account(user.getId(), email, login(email));
    }

    /** 관리자 사용자를 만들고 토큰과 id를 돌려준다. */
    public Account createAdmin() {
        String email = uniqueEmail();
        User user = userService.register(email, PASSWORD, uniqueNickname());
        userService.changeRole(user.getId(), Role.ADMIN);
        return new Account(user.getId(), email, login(email));
    }

    /** 일반 사용자 토큰만 필요할 때 */
    public String createUserAndLogin() {
        return createUser().accessToken();
    }

    /** 관리자 토큰만 필요할 때 */
    public String createAdminAndLogin() {
        return createAdmin().accessToken();
    }

    private String login(String email) {
        AuthResult result = authService.login(email, PASSWORD, "127.0.0.1", "test");
        return result.accessToken();
    }

    private static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String uniqueNickname() {
        return "닉" + UUID.randomUUID().toString().substring(0, 8);
    }
}
