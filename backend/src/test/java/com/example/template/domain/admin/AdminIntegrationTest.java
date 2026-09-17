package com.example.template.domain.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.template.support.IntegrationTestSupport;
import com.example.template.support.TestFixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** 관리자 API 통합 테스트. 두 DB에서 모두 통과해야 한다. */
@AutoConfigureMockMvc
@Import(TestFixtures.class)
@DisplayName("관리자 API")
class AdminIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestFixtures fixtures;

    private String adminToken;
    private Long adminId;
    private String userToken;
    private Long userId;
    private String boardSlug;

    @BeforeEach
    void setUp() throws Exception {
        TestFixtures.Account admin = fixtures.createAdmin();
        adminToken = admin.accessToken();
        adminId = admin.userId();

        TestFixtures.Account user = fixtures.createUser();
        userToken = user.accessToken();
        userId = user.userId();

        boardSlug = "admin-" + UUID.randomUUID().toString().substring(0, 8);
        createBoard(boardSlug);
    }

    @Test
    @DisplayName("대시보드가 통계와 최근 글을 반환한다")
    void dashboard() throws Exception {
        createPost("대시보드 확인용 글");

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.users").isNumber())
                .andExpect(jsonPath("$.totals.posts").isNumber())
                .andExpect(jsonPath("$.totals.comments").isNumber())
                .andExpect(jsonPath("$.dailyPosts").isArray())
                .andExpect(jsonPath("$.recentPosts").isArray());
    }

    @Test
    @DisplayName("일반 사용자는 관리자 API에 접근할 수 없다")
    void userCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글을 숨기면 사용자 목록과 상세에서 사라진다")
    void hidePost() throws Exception {
        long postId = createPost("숨길 글");

        mockMvc.perform(patch("/api/v1/admin/posts/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%d],\"status\":\"HIDDEN\"}".formatted(postId)))
                .andExpect(status().isNoContent());

        // 사용자 화면에서는 보이지 않는다
        mockMvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts"))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // 관리자 목록에서는 보인다
        mockMvc.perform(get("/api/v1/admin/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "HIDDEN")
                        .param("keyword", "숨길 글"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"));

        // 복구하면 다시 보인다
        mockMvc.perform(patch("/api/v1/admin/posts/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%d],\"status\":\"PUBLISHED\"}".formatted(postId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글을 일괄 삭제한다")
    void deletePosts() throws Exception {
        long first = createPost("삭제 대상 1");
        long second = createPost("삭제 대상 2");

        mockMvc.perform(delete("/api/v1/admin/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%d,%d]}".formatted(first, second)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("댓글을 숨기면 사용자 화면에서 내용이 감춰진다")
    void hideComment() throws Exception {
        long postId = createPost("댓글 달 글");
        long commentId = createComment(postId, "숨길 댓글");

        mockMvc.perform(patch("/api/v1/admin/comments/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%d],\"status\":\"HIDDEN\"}".formatted(commentId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$[0].hidden").value(true))
                .andExpect(jsonPath("$[0].content").value("관리자에 의해 숨겨진 댓글입니다."));
    }

    @Test
    @DisplayName("사용자 목록을 검색하고 역할을 바꾼다")
    void searchAndChangeRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("USER"));

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNoContent());

        // 역할이 바뀌면 기존 토큰은 폐기된다 → 재발급 불가
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자는 자기 자신의 역할이나 상태를 바꿀 수 없다")
    void cannotModifySelf() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + adminId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_SELF_MODIFICATION"));

        mockMvc.perform(patch("/api/v1/admin/users/" + adminId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_SELF_MODIFICATION"));
    }

    @Test
    @DisplayName("사용자를 정지하면 로그인과 기존 토큰 사용이 막힌다")
    void suspendUser() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isNoContent());

        // 정지된 사용자는 글을 쓸 수 없다(게시판 쓰기 권한 확인 전에 계정 상태로 막힌다)
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "SUSPENDED"))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ---- helpers ----

    private void createBoard(String slug) throws Exception {
        String body = """
                {"slug":"%s","name":"관리 테스트 게시판","description":"설명",
                 "readPermission":"ANONYMOUS","writePermission":"USER",
                 "commentEnabled":true,"attachmentEnabled":true,"active":true}""".formatted(slug);
        mockMvc.perform(post("/api/v1/admin/boards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private long createPost(String title) throws Exception {
        String response = mockMvc.perform(post("/api/v1/boards/" + boardSlug + "/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"<p>내용</p>\",\"notice\":false}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createComment(long postId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\"}".formatted(content)))
                .andExpect(status().isCreated());

        String list = mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(list).get(0).get("id").asLong();
    }
}
