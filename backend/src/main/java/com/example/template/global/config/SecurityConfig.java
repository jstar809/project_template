package com.example.template.global.config;

import com.example.template.global.security.JwtAuthenticationFilter;
import com.example.template.global.security.SecurityProblemHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 보안 설정.
 *
 * <p>세션을 만들지 않는 stateless 서버다(PRD SCALE-01). 인증은 JWT Access Token을 Authorization 헤더로 받는다.
 *
 * <p>CSRF는 끈다. 인증을 쿠키가 아닌 헤더로 하기 때문이다. 쿠키를 쓰는 refresh/logout은 SameSite=Strict 쿠키와 Origin 검사로
 * 보호한다(PRD NFR-SEC-02).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityProblemHandler securityProblemHandler)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .referrerPolicy(
                                referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        // 관리자 전용
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        // 그 외 API는 로그인 필요
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
