package com.example.template.support;

/** 테스트 대상 DB 벤더. 시스템 속성 {@code db.vendor}로 결정된다. */
public final class DbVendor {

    public static final String POSTGRESQL = "postgresql";
    public static final String MYSQL = "mysql";

    private DbVendor() {}

    public static String current() {
        return System.getProperty("db.vendor", POSTGRESQL);
    }

    public static boolean isMysql() {
        return MYSQL.equalsIgnoreCase(current());
    }
}
