package com.example.template.support;

import org.springframework.test.context.ActiveProfilesResolver;

/**
 * 테스트에 적용할 프로필을 정한다: {@code test} + {@code db-<벤더>}.
 *
 * <p>벤더는 Gradle이 넘긴 시스템 속성 {@code db.vendor}에서 읽는다. 실행 방법:
 *
 * <pre>
 *   ./gradlew test -PdbVendor=postgresql   (기본)
 *   ./gradlew test -PdbVendor=mysql
 * </pre>
 */
public class DbVendorProfileResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
        return new String[] {"test", "db-" + DbVendor.current()};
    }
}
