package com.example.template.domain.post.infra.mybatis;

import com.example.template.domain.post.application.PostListItem;
import com.example.template.domain.post.application.PostSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 게시글 조회 전용 Mapper (목록, 검색, 공지). */
@Mapper
public interface PostQueryMapper {

    /** 조건에 맞는 일반 글 목록(공지 제외). */
    List<PostListItem> selectPosts(@Param("condition") PostSearchCondition condition);

    /** 조건에 맞는 일반 글 개수. 페이지 수 계산에 쓴다. */
    long countPosts(@Param("condition") PostSearchCondition condition);

    /** 게시판 상단에 고정할 공지 목록. 페이지와 무관하게 항상 보여준다. */
    List<PostListItem> selectNotices(@Param("boardId") Long boardId);
}
