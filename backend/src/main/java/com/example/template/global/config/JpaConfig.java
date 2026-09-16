package com.example.template.global.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing 설정. createdAt/updatedAt/createdBy를 자동으로 채운다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 현재 로그인 사용자 id를 반환한다.
     *
     * <p>M2에서 SecurityContext의 인증 주체를 읽도록 교체한다. 지금은 인증 기능이 없어 항상 비어 있다.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return Optional::empty;
    }
}
