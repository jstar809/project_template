package com.example.template.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 공통 부모.
 *
 * <p>Testcontainers로 실제 DB를 띄운다. 어떤 DB를 띄울지는 {@code db.vendor} 시스템 속성이 정한다. 컨테이너는 JVM 당 한 번만 띄우고
 * 여러 테스트가 공유한다(매번 띄우면 느리기 때문).
 *
 * <p>같은 테스트가 PostgreSQL과 MySQL 양쪽에서 모두 통과해야 한다(PRD FR-QUALITY-03).
 */
@SpringBootTest
@ActiveProfiles(resolver = DbVendorProfileResolver.class)
@Tag("integration")
public abstract class IntegrationTestSupport {

    @SuppressWarnings("rawtypes")
    private static final JdbcDatabaseContainer CONTAINER = createContainer();

    static {
        CONTAINER.start();
    }

    @SuppressWarnings("rawtypes")
    private static JdbcDatabaseContainer createContainer() {
        if (DbVendor.isMysql()) {
            return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                    .withCommand(
                            "--character-set-server=utf8mb4",
                            "--collation-server=utf8mb4_0900_ai_ci",
                            "--default-time-zone=+00:00");
        }
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    /** 컨테이너의 접속 정보를 애플리케이션 설정(DB_HOST 등)으로 넘긴다. */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        int internalPort = DbVendor.isMysql() ? 3306 : 5432;
        registry.add("DB_HOST", CONTAINER::getHost);
        registry.add("DB_PORT", () -> CONTAINER.getMappedPort(internalPort));
        registry.add("DB_NAME", CONTAINER::getDatabaseName);
        registry.add("DB_USERNAME", CONTAINER::getUsername);
        registry.add("DB_PASSWORD", CONTAINER::getPassword);
    }
}
