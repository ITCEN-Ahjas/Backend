package com.example.Chungbuk.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI chungbukOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chungbuk Travel API")
                        .description("충청북도 축제/체험/숙박 정보를 제공하는 API 문서입니다.")
                        .version("v1"));
    }
}
