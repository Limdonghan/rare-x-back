package com.project.rare_x_back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 보안 요구사항 설정 (JWT라는 이름의 보안 체계를 사용하겠다는 뜻)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("JWT");

        // 2. 보안 체계 정의 (HTTP Bearer 방식의 JWT 설정)
        Components components = new Components().addSecuritySchemes("JWT",
                new SecurityScheme()
                        .name("JWT")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
        );

        return new OpenAPI()
                .info(new Info()
                        .title("RARE-X") // 우리 프로젝트 이름
                        .description("레전드 중고거래 플랫폼") // 설명
                        .version("1.0")) // 버전
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}
