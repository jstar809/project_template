package com.example.template.domain.user.application;

import com.example.template.global.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 기동 시 초기 관리자 계정을 만든다.
 *
 * <p>비밀번호를 해시해야 하므로 SQL 시드가 아니라 코드로 만든다(decisions D-008).
 *
 * <p>인스턴스를 여러 개 동시에 띄워도 안전하다. 이메일 유니크 제약이 중복 생성을 막고, 그때 발생하는 예외는 무시한다(PRD SCALE-10).
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserService userService;
    private final AppProperties appProperties;

    public AdminAccountInitializer(UserService userService, AppProperties appProperties) {
        this.userService = userService;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = appProperties.admin();
        if (!admin.isConfigured()) {
            log.info("관리자 계정 설정(ADMIN_EMAIL/ADMIN_PASSWORD)이 없어 초기 관리자를 만들지 않는다.");
            return;
        }
        if (userService.findByEmail(admin.email()).isPresent()) {
            return;
        }
        try {
            userService.registerAdmin(admin.email(), admin.password(), admin.nickname());
            log.info("초기 관리자 계정을 생성했다: {}", admin.email());
        } catch (DataIntegrityViolationException e) {
            // 다른 인스턴스가 먼저 만들었다. 정상 상황이다.
            log.info("초기 관리자 계정은 이미 존재한다: {}", admin.email());
        }
    }
}
