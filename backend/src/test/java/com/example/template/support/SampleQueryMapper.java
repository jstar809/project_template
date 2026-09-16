package com.example.template.support;

import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis 설정 검증용 샘플 Mapper(테스트 전용).
 *
 * <p>{@code currentVendor()}는 XML에서 {@code databaseId}로 분기되어, DB마다 다른 SQL이 선택되는지 확인한다.
 */
@Mapper
public interface SampleQueryMapper {

    /** DB별로 분기된 SQL이 반환하는 벤더 이름 */
    String currentVendor();

    /** 시드된 게시판 수 (표준 SQL, 두 DB 공통) */
    long countBoards();

    /** ShedLock 테이블이 존재하는지 (표준 SQL, 두 DB 공통) */
    long countShedlock();
}
