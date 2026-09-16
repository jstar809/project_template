package com.example.template.domain.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.template.domain.board.domain.Permission;
import com.example.template.domain.user.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 게시판 권한 판정 단위 테스트 (PRD FR-BOARD-06). */
@DisplayName("게시판 권한")
class PermissionTest {

    @ParameterizedTest(name = "요구={0}, 사용자={1} → 허용={2}")
    @CsvSource({
        // 비로그인(ANONYMOUS)
        "ANONYMOUS, , true",
        "USER, , false",
        "ADMIN, , false",
        // 일반 사용자
        "ANONYMOUS, USER, true",
        "USER, USER, true",
        "ADMIN, USER, false",
        // 관리자는 모든 단계를 만족한다
        "ANONYMOUS, ADMIN, true",
        "USER, ADMIN, true",
        "ADMIN, ADMIN, true",
    })
    @DisplayName("사용자 단계가 요구 단계 이상이면 허용한다")
    void allows(Permission required, String roleName, boolean expected) {
        Role role = roleName == null || roleName.isBlank() ? null : Role.valueOf(roleName);

        assertThat(required.allows(role)).isEqualTo(expected);
    }
}
