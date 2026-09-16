package com.example.template.domain.post.application;

import com.example.template.domain.board.domain.Board;
import com.example.template.domain.file.domain.StoredFile;
import com.example.template.domain.post.domain.Post;
import java.util.List;

/** 상세 화면에 필요한 값 묶음. api 계층에서 응답 DTO로 변환한다. */
public record PostDetail(
        Post post,
        Board board,
        String authorNickname,
        List<StoredFile> attachments,
        boolean canEdit,
        boolean canDelete) {}
