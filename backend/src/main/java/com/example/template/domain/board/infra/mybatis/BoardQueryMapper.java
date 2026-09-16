package com.example.template.domain.board.infra.mybatis;

import com.example.template.domain.board.application.AdminBoardSummary;
import com.example.template.domain.board.application.BoardSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 게시판 조회 전용 Mapper (PRD 8.2: 목록 조회는 MyBatis). */
@Mapper
public interface BoardQueryMapper {

    /**
     * 현재 사용자가 읽을 수 있는 활성 게시판 목록.
     *
     * @param allowedPermissions 사용자가 만족하는 권한 값들 (예: USER면 ANONYMOUS, USER)
     */
    List<BoardSummary> selectReadableBoards(@Param("allowedPermissions") List<String> allowedPermissions);

    /** 관리자 목록: 비활성 포함, 게시글 수 포함 */
    List<AdminBoardSummary> selectAdminBoards();
}
