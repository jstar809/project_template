package com.example.template.global.config;

import com.example.template.global.security.AuthPrincipal;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** JPA Auditing 설정. createdAt/updatedAt/createdBy를 자동으로 채운다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /** 현재 로그인 사용자 id를 반환한다. 비로그인 요청이면 비어 있다. */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            if (authentication.getPrincipal() instanceof AuthPrincipal principal) {
                return Optional.of(principal.userId());
            }
            return Optional.empty();
        };
    }
}
