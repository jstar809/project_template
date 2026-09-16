package com.example.template.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.template.domain.user.application.UserService;
import com.example.template.domain.user.domain.User;
import com.example.template.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 인증 API 통합 테스트. PostgreSQL과 MySQL 양쪽에서 동일하게 통과해야 한다. */
@AutoConfigureMockMvc
@DisplayName("인증 API")
class AuthIntegrationTest extends IntegrationTestSupport {

    private static final String REFRESH_COOKIE = "refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("회원가입 후 로그인하면 Access Token과 Refresh Token 쿠키를 받는다")
    void signupAndLogin() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());

        // when
        MvcResult result =
                login(email, "password123").andExpect(status().isOk()).andReturn();

        // then
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("user").get("email").asText()).isEqualTo(email);

        Cookie refreshCookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/v1/auth");
    }

    @Test
    @DisplayName("Access Token으로 내 정보를 조회한다")
    void getMeWithAccessToken() throws Exception {
        String email = uniqueEmail();
        String nickname = uniqueNickname();
        signup(email, "password123", nickname);
        String accessToken = accessToken(login(email, "password123").andReturn());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nickname").value(nickname))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("토큰 없이 보호된 API를 호출하면 401과 ProblemDetail을 받는다")
    void unauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("중복 이메일로 가입하면 409를 받는다")
    void duplicatedEmail() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email, "password123", uniqueNickname())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_DUPLICATED"));
    }

    @Test
    @DisplayName("비밀번호가 규칙에 맞지 않으면 400과 필드 오류를 받는다")
    void invalidPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(uniqueEmail(), "short", uniqueNickname())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("재발급하면 새 토큰이 나오고 기존 refresh token은 폐기된다(rotation)")
    void refreshRotatesToken() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());
        MvcResult loginResult = login(email, "password123").andReturn();
        Cookie firstCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE);

        // when: 첫 번째 토큰으로 재발급
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isOk())
                .andReturn();

        // then: 새 쿠키가 발급되고 값이 다르다
        Cookie secondCookie = refreshResult.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(secondCookie).isNotNull();
        assertThat(secondCookie.getValue()).isNotEqualTo(firstCookie.getValue());
        assertThat(accessToken(refreshResult)).isNotBlank();
    }

    @Test
    @DisplayName("폐기된 refresh token을 재사용하면 해당 사용자의 모든 토큰이 폐기된다")
    void reuseDetectionRevokesAllTokens() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());
        Cookie firstCookie =
                login(email, "password123").andReturn().getResponse().getCookie(REFRESH_COOKIE);

        MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andReturn();
        Cookie secondCookie = rotated.getResponse().getCookie(REFRESH_COOKIE);

        // when: 이미 폐기된 첫 번째 토큰을 다시 사용 (탈취 시나리오)
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));

        // then: 정상이던 두 번째 토큰도 함께 폐기되어 더는 쓸 수 없다
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(secondCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 refresh token이 폐기되고 쿠키가 삭제된다")
    void logout() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());
        Cookie refreshCookie =
                login(email, "password123").andReturn().getResponse().getCookie(REFRESH_COOKIE);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("정지된 계정은 로그인과 재발급이 모두 막힌다")
    void suspendedUserCannotLoginOrRefresh() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());
        Cookie refreshCookie =
                login(email, "password123").andReturn().getResponse().getCookie(REFRESH_COOKIE);

        // when: 계정 정지
        User user = userService.findByEmail(email).orElseThrow();
        userService.suspend(user.getId());

        // then
        login(email, "password123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_SUSPENDED"));
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 기존 refresh token이 폐기된다")
    void changePasswordRevokesTokens() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());
        MvcResult loginResult = login(email, "password123").andReturn();
        String accessToken = accessToken(loginResult);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE);

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"newpassword123"}"""))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());

        login(email, "newpassword123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인을 반복해서 실패하면 429를 받는다")
    void loginRateLimit() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123", uniqueNickname());

        // 설정된 한도(10회)까지는 401
        for (int i = 0; i < 10; i++) {
            login(email, "wrongpassword").andExpect(status().isUnauthorized());
        }

        // 한도를 넘으면 429
        login(email, "wrongpassword")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_TOO_MANY_ATTEMPTS"));
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.ResultActions signup(String email, String password, String nickname)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email, password, nickname)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String signupBody(String email, String password, String nickname) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}".formatted(email, password, nickname);
    }

    private String accessToken(MvcResult result) throws Exception {
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String uniqueNickname() {
        return "닉" + UUID.randomUUID().toString().substring(0, 8);
    }
}
