package com.example.template.domain.post.application;

import java.util.List;

/**
 * 게시글 작성/수정 입력.
 *
 * @param fileIds 연결할 파일 전체 (본문 이미지 + 첨부)
 * @param inlineFileIds 그중 본문에 삽입된 이미지 id
 */
public record PostCommand(String title, String content, boolean notice, List<Long> fileIds, List<Long> inlineFileIds) {}
