package com.example.template.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 보안 설정.
 *
 * <p>세션을 만들지 않는 stateless 서버다(PRD SCALE-01). 인증은 M2에서 JWT 필터로 붙인다. 지금은 인증이 필요한 API가 없어 모든 요청을
 * 허용한다.
 *
 * <p>CSRF는 끈다. API 인증을 Authorization 헤더로 하기 때문이다. 쿠키를 쓰는 refresh/logout은 M2에서 SameSite 쿠키와 Origin
 * 검사로 보호한다(PRD NFR-SEC-02).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(
                                referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
