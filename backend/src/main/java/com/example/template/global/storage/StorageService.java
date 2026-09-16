package com.example.template.global.storage;

import java.io.InputStream;

/**
 * 파일 저장소 추상화.
 *
 * <p>구현을 바꾸면(로컬 디스크 → S3) 서비스 코드는 그대로 둘 수 있다(PRD SCALE-02). 저장 키는 {@code
 * yyyy/MM/dd/{UUID}.{확장자}} 형식이며, 원본 파일명은 DB에만 둔다.
 */
public interface StorageService {

    /** 파일을 저장한다. 같은 키가 있으면 덮어쓴다. */
    void store(String key, InputStream content, String contentType);

    /** 저장된 파일을 읽는다. 없으면 예외를 던진다. */
    InputStream load(String key);

    /** 파일을 삭제한다. 없으면 아무 일도 하지 않는다. */
    void delete(String key);

    boolean exists(String key);
}
