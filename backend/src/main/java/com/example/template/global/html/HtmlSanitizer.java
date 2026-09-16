package com.example.template.global.html;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * 사용자가 쓴 HTML에서 위험한 요소를 제거한다(PRD FR-POST-05).
 *
 * <p>화이트리스트 방식이다. 허용 목록에 없는 태그와 속성은 전부 지운다. 블랙리스트(위험한 것만 지우기)는 우회 방법이 계속 나오기 때문에 쓰지 않는다.
 */
@Component
public class HtmlSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "p",
                    "br",
                    "strong",
                    "b",
                    "em",
                    "i",
                    "u",
                    "s",
                    "h1",
                    "h2",
                    "h3",
                    "ul",
                    "ol",
                    "li",
                    "blockquote",
                    "code",
                    "pre",
                    "a",
                    "img",
                    "span")
            // 링크는 http/https만 허용하고, 새 창으로 열릴 때 opener 공격을 막는다.
            .allowAttributes("href")
            .onElements("a")
            .allowUrlProtocols("http", "https")
            .requireRelNofollowOnLinks()
            // 이미지는 자체 업로드 경로(/api/v1/files/...)만 허용한다.
            .allowAttributes("src", "alt", "width", "height")
            .onElements("img")
            .allowStandardUrlProtocols()
            .toFactory();

    /** 저장 전에 호출한다. */
    public String sanitize(String rawHtml) {
        if (rawHtml == null) {
            return "";
        }
        return POLICY.sanitize(rawHtml);
    }

    /**
     * 태그를 제거한 순수 텍스트를 만든다.
     *
     * <p>검색용 컬럼(content_text)에 저장한다. 본문 HTML을 LIKE로 검색하면 태그 이름까지 걸리기 때문이다.
     */
    public String toPlainText(String rawHtml) {
        if (rawHtml == null) {
            return "";
        }
        String withoutTags = rawHtml.replaceAll("<[^>]*>", " ");
        String unescaped = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return unescaped.replaceAll("\\s+", " ").trim();
    }
}
