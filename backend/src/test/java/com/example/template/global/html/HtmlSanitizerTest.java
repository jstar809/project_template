package com.example.template.global.html;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HTML 정제 단위 테스트 (PRD FR-POST-05, NFR-SEC-01). */
@DisplayName("HTML 정제")
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    @DisplayName("script 태그를 제거한다")
    void removesScriptTag() {
        String result = sanitizer.sanitize("<p>안녕하세요</p><script>alert('xss')</script>");

        assertThat(result).contains("안녕하세요");
        assertThat(result).doesNotContain("script");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    @DisplayName("이벤트 핸들러 속성을 제거한다")
    void removesEventHandlers() {
        String result = sanitizer.sanitize("<img src=\"/api/v1/files/1\" onerror=\"alert('xss')\">");

        assertThat(result).doesNotContain("onerror");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    @DisplayName("javascript: 링크를 제거한다")
    void removesJavascriptUrl() {
        String result = sanitizer.sanitize("<a href=\"javascript:alert(1)\">클릭</a>");

        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("iframe과 style 태그를 제거한다")
    void removesDangerousElements() {
        String result = sanitizer.sanitize("<iframe src=\"http://evil.com\"></iframe><style>body{}</style><p>본문</p>");

        assertThat(result).doesNotContain("iframe");
        assertThat(result).doesNotContain("<style");
        assertThat(result).contains("본문");
    }

    @Test
    @DisplayName("허용된 서식 태그는 유지한다")
    void keepsAllowedTags() {
        String result = sanitizer.sanitize("<p><strong>굵게</strong> <em>기울임</em></p><ul><li>항목</li></ul>");

        assertThat(result).contains("<strong>").contains("<em>").contains("<li>");
    }

    @Test
    @DisplayName("태그를 제거한 검색용 텍스트를 만든다")
    void convertsToPlainText() {
        String result = sanitizer.toPlainText("<p>첫 문단</p><p>둘째 <strong>문단</strong></p>");

        assertThat(result).isEqualTo("첫 문단 둘째 문단");
        assertThat(result).doesNotContain("<");
    }
}
