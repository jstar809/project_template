package com.example.template.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 게시판/게시글/댓글/파일 통합 테스트. 두 DB에서 모두 통과해야 한다. */
@AutoConfigureMockMvc
@Import(TestFixtures.class)
@DisplayName("CMS API")
class CmsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestFixtures fixtures;

    private String adminToken;
    private String userToken;
    private String otherUserToken;
    private String boardSlug;

    @BeforeEach
    void setUp() {
        adminToken = fixtures.createAdminAndLogin();
        userToken = fixtures.createUserAndLogin();
        otherUserToken = fixtures.createUserAndLogin();
        boardSlug = "board-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---- 게시판 ----

    @Test
    @DisplayName("관리자가 게시판을 만들면 사용자 목록에 나타난다")
    void adminCreatesBoard() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);

        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='" + boardSlug + "')]").exists());
    }

    @Test
    @DisplayName("일반 사용자는 게시판을 만들 수 없다")
    void userCannotCreateBoard() throws Exception {
        mockMvc.perform(post("/api/v1/admin/boards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(boardBody(boardSlug, "ANONYMOUS", "USER", true, true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("읽기 권한이 USER인 게시판은 비로그인 사용자에게 보이지 않는다")
    void memberOnlyBoardHiddenFromAnonymous() throws Exception {
        createBoard(boardSlug, "USER", "USER", true, true);

        // 목록에서 제외된다
        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='" + boardSlug + "')]").doesNotExist());

        // 직접 접근하면 403
        mockMvc.perform(get("/api/v1/boards/" + boardSlug)).andExpect(status().isForbidden());

        // 로그인하면 볼 수 있다
        mockMvc.perform(get("/api/v1/boards/" + boardSlug).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쓰기 권한이 ADMIN인 게시판에는 일반 사용자가 글을 쓸 수 없다")
    void userCannotWriteToAdminBoard() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "ADMIN", true, true);

        mockMvc.perform(post("/api/v1/boards/" + boardSlug + "/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("제목", "<p>내용</p>", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BOARD_WRITE_FORBIDDEN"));
    }

    @Test
    @DisplayName("게시글이 있는 게시판은 삭제할 수 없다")
    void cannotDeleteBoardWithPosts() throws Exception {
        long boardId = createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        createPost(userToken, boardSlug, "제목", "<p>내용</p>", false);

        mockMvc.perform(delete("/api/v1/admin/boards/" + boardId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOARD_HAS_POSTS"));
    }

    // ---- 게시글 ----

    @Test
    @DisplayName("글을 쓰고 목록과 상세에서 확인한다")
    void writeAndReadPost() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "첫 글입니다", "<p>본문입니다</p>", false);

        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].title").value("첫 글입니다"));

        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("첫 글입니다"))
                .andExpect(jsonPath("$.content").value("<p>본문입니다</p>"))
                .andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    @DisplayName("본문의 위험한 HTML은 저장 전에 제거된다")
    void sanitizesContent() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "XSS 시도", "<p>정상</p><script>alert('xss')</script>", false);

        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("정상")))
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("script"))));
    }

    @Test
    @DisplayName("제목과 내용으로 검색한다")
    void searchPosts() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        createPost(userToken, boardSlug, "봄 소식", "<p>벚꽃이 피었습니다</p>", false);
        createPost(userToken, boardSlug, "겨울 소식", "<p>눈이 내립니다</p>", false);

        // 제목 검색
        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts")
                        .param("searchType", "title")
                        .param("keyword", "봄"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].title").value("봄 소식"));

        // 내용 검색 (태그를 제거한 텍스트 대상)
        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts")
                        .param("searchType", "content")
                        .param("keyword", "벚꽃"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // 없는 단어
        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts")
                        .param("searchType", "title_content")
                        .param("keyword", "가을"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("공지는 목록 상단에 따로 반환되고 일반 목록에는 포함되지 않는다")
    void noticesAreSeparated() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        createPost(adminToken, boardSlug, "공지사항", "<p>공지 내용</p>", true);
        createPost(userToken, boardSlug, "일반 글", "<p>일반 내용</p>", false);

        mockMvc.perform(get("/api/v1/boards/" + boardSlug + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(1))
                .andExpect(jsonPath("$.notices[0].title").value("공지사항"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].title").value("일반 글"));
    }

    @Test
    @DisplayName("일반 사용자는 공지로 글을 쓸 수 없다")
    void userCannotCreateNotice() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);

        mockMvc.perform(post("/api/v1/boards/" + boardSlug + "/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("공지 시도", "<p>내용</p>", true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_NOTICE_FORBIDDEN"));
    }

    @Test
    @DisplayName("남의 글은 수정하거나 삭제할 수 없고, 관리자는 할 수 있다")
    void onlyAuthorOrAdminCanEdit() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "내 글", "<p>내용</p>", false);

        mockMvc.perform(put("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("남의 글 수정", "<p>바꿈</p>", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_EDIT_FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/posts/" + postId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
    }

    // ---- 댓글 ----

    @Test
    @DisplayName("댓글과 대댓글을 달고 목록에서 트리로 받는다")
    void writeCommentAndReply() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "글", "<p>내용</p>", false);

        long commentId = createComment(otherUserToken, postId, "첫 댓글", null);
        createComment(userToken, postId, "답글입니다", commentId);

        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$[0].replies.length()").value(1))
                .andExpect(jsonPath("$[0].replies[0].content").value("답글입니다"));

        // 댓글 수가 게시글에 반영된다
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(jsonPath("$.commentCount").value(2));
    }

    @Test
    @DisplayName("대댓글에는 답글을 달 수 없다")
    void cannotReplyToReply() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "글", "<p>내용</p>", false);
        long commentId = createComment(userToken, postId, "댓글", null);
        long replyId = createComment(userToken, postId, "대댓글", commentId);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"대대댓글\",\"parentId\":" + replyId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_DEPTH_EXCEEDED"));
    }

    @Test
    @DisplayName("댓글을 허용하지 않는 게시판에는 댓글을 달 수 없다")
    void cannotCommentWhenDisabled() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", false, true);
        long postId = createPost(userToken, boardSlug, "글", "<p>내용</p>", false);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_DISABLED"));
    }

    @Test
    @DisplayName("삭제된 댓글은 자리만 남고 내용이 감춰진다")
    void deletedCommentIsMasked() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long postId = createPost(userToken, boardSlug, "글", "<p>내용</p>", false);
        long commentId = createComment(userToken, postId, "지울 댓글", null);
        createComment(otherUserToken, postId, "답글", commentId);

        mockMvc.perform(delete("/api/v1/comments/" + commentId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$[0].deleted").value(true))
                .andExpect(jsonPath("$[0].content").value("삭제된 댓글입니다."))
                .andExpect(jsonPath("$[0].replies.length()").value(1));
    }

    // ---- 파일 ----

    @Test
    @DisplayName("이미지를 올려 글에 첨부하면 상세에서 확인할 수 있다")
    void uploadAndAttachFile() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, true);
        long fileId = uploadPng(userToken, "photo.png");

        String body = """
                {"title":"첨부 글","content":"<p>본문</p>","notice":false,"fileIds":[%d],"inlineFileIds":[]}""".formatted(fileId);
        String response = mockMvc.perform(post("/api/v1/boards/" + boardSlug + "/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("attachments").size()).isEqualTo(1);
        assertThat(json.get("attachments").get(0).get("originalName").asText()).isEqualTo("photo.png");

        // 다운로드도 된다
        mockMvc.perform(get("/api/v1/files/" + fileId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("확장자를 위장한 파일은 업로드를 거부한다")
    void rejectsDisguisedFile() throws Exception {
        byte[] exeBytes = {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", exeBytes);

        mockMvc.perform(multipart("/api/v1/files").file(file).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("남이 올린 임시 파일은 다운로드할 수 없다")
    void cannotDownloadOthersTempFile() throws Exception {
        long fileId = uploadPng(userToken, "private.png");

        mockMvc.perform(get("/api/v1/files/" + fileId).header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_NOT_OWNED"));
    }

    @Test
    @DisplayName("첨부를 허용하지 않는 게시판에는 파일을 붙일 수 없다")
    void cannotAttachWhenDisabled() throws Exception {
        createBoard(boardSlug, "ANONYMOUS", "USER", true, false);
        long fileId = uploadPng(userToken, "photo.png");

        String body = """
                {"title":"첨부 시도","content":"<p>본문</p>","notice":false,"fileIds":[%d]}""".formatted(fileId);
        mockMvc.perform(post("/api/v1/boards/" + boardSlug + "/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_ATTACHMENT_DISABLED"));
    }

    // ---- helpers ----

    private long createBoard(
            String slug,
            String readPermission,
            String writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/boards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(boardBody(slug, readPermission, writePermission, commentEnabled, attachmentEnabled)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String boardBody(
            String slug,
            String readPermission,
            String writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled) {
        return """
               {"slug":"%s","name":"테스트 게시판","description":"설명",
                "readPermission":"%s","writePermission":"%s",
                "commentEnabled":%s,"attachmentEnabled":%s,"active":true}""".formatted(slug, readPermission, writePermission, commentEnabled, attachmentEnabled);
    }

    private long createPost(String token, String slug, String title, String content, boolean notice) throws Exception {
        String response = mockMvc.perform(post("/api/v1/boards/" + slug + "/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(title, content, notice)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String postBody(String title, String content, boolean notice) {
        return "{\"title\":\"%s\",\"content\":\"%s\",\"notice\":%s}"
                .formatted(title, content.replace("\"", "\\\""), notice);
    }

    private long createComment(String token, long postId, String content, Long parentId) throws Exception {
        String body = parentId == null
                ? "{\"content\":\"%s\"}".formatted(content)
                : "{\"content\":\"%s\",\"parentId\":%d}".formatted(content, parentId);
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 방금 만든 댓글 id는 목록의 마지막 항목에서 찾는다.
        String list = mockMvc.perform(
                        get("/api/v1/posts/" + postId + "/comments").header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode nodes = objectMapper.readTree(list);
        JsonNode last = nodes.get(nodes.size() - 1);
        JsonNode replies = last.get("replies");
        if (parentId != null && replies.size() > 0) {
            return replies.get(replies.size() - 1).get("id").asLong();
        }
        return last.get("id").asLong();
    }

    private long uploadPng(String token, String fileName) throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        MockMultipartFile file = new MockMultipartFile("file", fileName, "image/png", pngBytes);

        String response = mockMvc.perform(
                        multipart("/api/v1/files").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
