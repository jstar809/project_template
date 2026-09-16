package com.example.template.global.config;

import java.util.Properties;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 설정.
 *
 * <p>조회(목록, 검색, 통계)는 MyBatis가 담당하고, 쓰기는 JPA가 담당한다(PRD 8.2). 두 기술은 같은 DataSource와 같은 트랜잭션을
 * 공유한다.
 *
 * <p>{@link VendorDatabaseIdProvider}를 등록하면 XML에서 {@code databaseId="mysql"} 처럼 DB별로 다른 SQL을 쓸 수
 * 있다. 벤더 차이가 있는 구문(페이징 힌트, upsert, 날짜 함수)만 분기하고 나머지는 표준 SQL로 작성한다.
 */
@Configuration
@MapperScan(basePackages = "com.example.template.domain", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class MyBatisConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("PostgreSQL", "postgresql");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("MariaDB", "mysql");
        provider.setProperties(properties);
        return provider;
    }
}
