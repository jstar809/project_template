package com.example.template.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger(OpenAPI) 문서 설정. JWT Bearer 인증 스키마를 등록해 Authorize 버튼을 쓸 수 있게 한다. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme scheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 API로 받은 accessToken을 입력한다.");

        return new OpenAPI()
                .info(new Info()
                        .title("Fullstack Template API")
                        .version("v1")
                        .description("Spring Boot + React 템플릿 API 문서"))
                .components(new Components().addSecuritySchemes(BEARER, scheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
