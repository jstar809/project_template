package com.example.template.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 애플리케이션 고유 설정 (prefix: app).
 *
 * @param cors 허용 출처
 * @param cookie 인증 쿠키 설정
 * @param admin 초기 관리자 계정 설정
 * @param rateLimit 호출 제한 설정
 * @param storage 파일 저장소 설정
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Cookie cookie, Admin admin, RateLimit rateLimit, Storage storage) {

    public AppProperties {
        cors = cors == null ? new Cors(List.of("http://localhost:5173")) : cors;
        cookie = cookie == null ? new Cookie(false, "Strict") : cookie;
        admin = admin == null ? new Admin(null, null, null) : admin;
        rateLimit = rateLimit == null ? new RateLimit(10) : rateLimit;
        storage = storage == null ? new Storage("local", "./uploads") : storage;
    }

    /** CORS 허용 출처. 쉼표로 구분한 문자열을 넣으면 목록으로 변환된다. */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    /**
     * @param secure 운영에서는 true (HTTPS 전용 쿠키)
     * @param sameSite 기본 Strict. 다른 도메인에서 호출해야 하면 None + secure 조합이 필요하다.
     */
    public record Cookie(boolean secure, String sameSite) {
        public Cookie {
            sameSite = sameSite == null ? "Strict" : sameSite;
        }
    }

    /** 값이 모두 있으면 기동 시 관리자 계정을 만든다. */
    public record Admin(String email, String password, String nickname) {
        public Admin {
            nickname = nickname == null || nickname.isBlank() ? "관리자" : nickname;
        }

        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }

    /**
     * @param login 5분당 허용할 로그인 시도 횟수
     */
    public record RateLimit(int login) {
        public RateLimit {
            login = login <= 0 ? 10 : login;
        }
    }

    /**
     * @param type 저장소 구현 (local)
     * @param localPath 로컬 저장 경로
     */
    public record Storage(String type, String localPath) {
        public Storage {
            type = type == null ? "local" : type;
            localPath = localPath == null ? "./uploads" : localPath;
        }
    }
}
