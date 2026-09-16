package com.example.template.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;

/** 테스트 전용 Mapper를 스캔한다. 운영 코드의 @MapperScan은 domain 패키지만 본다. */
@TestConfiguration(proxyBeanMethods = false)
@MapperScan(basePackages = "com.example.template.support")
public class TestMapperConfig {}
