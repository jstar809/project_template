package com.example.template.domain.comment.infra.mybatis;

import com.example.template.domain.comment.application.CommentItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 댓글 조회 전용 Mapper. */
@Mapper
public interface CommentQueryMapper {

    /** 게시글의 댓글 전체(대댓글 포함)를 작성 순서로 가져온다. 트리 구성은 서비스에서 한다. */
    List<CommentItem> selectCommentsByPostId(@Param("postId") Long postId);
}
