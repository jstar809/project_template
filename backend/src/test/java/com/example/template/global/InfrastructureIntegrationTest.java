package com.example.template.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.template.support.DbVendor;
import com.example.template.support.IntegrationTestSupport;
import com.example.template.support.SampleQueryMapper;
import com.example.template.support.TestMapperConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** M1 공통 인프라가 두 DB에서 똑같이 동작하는지 확인한다. */
@Import(TestMapperConfig.class)
@AutoConfigureMockMvc
@DisplayName("공통 인프라 통합 테스트")
class InfrastructureIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SampleQueryMapper sampleQueryMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Flyway 마이그레이션이 적용되고 기본 게시판이 시드된다")
    void flywayMigrationApplied() {
        // when
        long boards = sampleQueryMapper.countBoards();

        // then: V2__seed.sql 이 공지사항/자유게시판 2개를 넣는다
        assertThat(boards).isEqualTo(2);
    }

    @Test
    @DisplayName("ShedLock 테이블이 생성된다")
    void shedlockTableCreated() {
        assertThat(sampleQueryMapper.countShedlock()).isZero();
    }

    @Test
    @DisplayName("MyBatis가 DB 벤더에 맞는 SQL을 고른다")
    void mybatisSelectsVendorSpecificSql() {
        // given: 실행 중인 DB 벤더
        String expected = DbVendor.current();

        // when: XML에서 databaseId로 분기된 쿼리를 실행
        String actual = sampleQueryMapper.currentVendor();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("없는 경로를 요청하면 ProblemDetail 형식에 code와 traceId가 포함된다")
    void unknownPathReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("헬스 체크가 UP을 반환한다")
    void healthEndpointUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
