package com.example.template.domain.file.domain;

/**
 * 파일 상태.
 *
 * <p>업로드 직후에는 TEMP다. 게시글에 연결되면 LINKED가 되고, TEMP로 남은 파일은 스케줄러가 정리한다.
 */
public enum FileStatus {
    TEMP,
    LINKED
}
