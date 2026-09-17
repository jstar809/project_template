package com.example.template.domain.admin.infra.mybatis;

import com.example.template.domain.admin.application.AdminDtos.AdminCommentItem;
import com.example.template.domain.admin.application.AdminDtos.AdminPostItem;
import com.example.template.domain.admin.application.AdminDtos.AdminUserItem;
import com.example.template.domain.admin.application.AdminDtos.CommentSearch;
import com.example.template.domain.admin.application.AdminDtos.DailyCount;
import com.example.template.domain.admin.application.AdminDtos.PostSearch;
import com.example.template.domain.admin.application.AdminDtos.RecentPost;
import com.example.template.domain.admin.application.AdminDtos.Totals;
import com.example.template.domain.admin.application.AdminDtos.UserSearch;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관리자 화면 조회 전용 Mapper.
 *
 * <p>여러 테이블을 조인하고 집계하는 쿼리라 MyBatis로 직접 작성한다(PRD 8.2). 관리자 화면은 숨김·삭제된 자료까지 모두 볼 수 있어야 하므로, 사용자 화면
 * 쿼리와 조건이 다르다.
 */
@Mapper
public interface AdminQueryMapper {

    Totals selectTotals();

    /** 최근 N일 일별 가입자 수 */
    List<DailyCount> selectDailySignups(@Param("from") Instant from);

    /** 최근 N일 일별 게시글 수 */
    List<DailyCount> selectDailyPosts(@Param("from") Instant from);

    List<RecentPost> selectRecentPosts(@Param("limit") int limit);

    List<AdminPostItem> selectPosts(@Param("condition") PostSearch condition);

    long countPosts(@Param("condition") PostSearch condition);

    List<AdminCommentItem> selectComments(@Param("condition") CommentSearch condition);

    long countComments(@Param("condition") CommentSearch condition);

    List<AdminUserItem> selectUsers(@Param("condition") UserSearch condition);

    long countUsers(@Param("condition") UserSearch condition);
}
