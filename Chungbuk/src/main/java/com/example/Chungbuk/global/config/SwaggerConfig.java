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
                        .description("""
                                충청북도 축제/체험/숙박 정보를 제공하는 API 문서입니다.

                                축제/체험 조회 API는 DB에 저장된 데이터를 기준으로 응답합니다.
                                TourAPI는 프론트엔드 요청마다 실시간 호출하지 않고, 수동 동기화 API를 통해 DB를 저장/갱신하는 데이터 소스로 사용합니다.

                                주요 흐름:
                                1. POST /api/festivals/sync 또는 POST /api/festivals/sync/{contentId}/detail
                                   → TourAPI 데이터를 festival_contents 테이블에 저장/갱신
                                2. GET /api/festivals, GET /api/festivals/experiences, GET /api/festivals/{contentId}
                                   → DB 데이터를 기준으로 조회 응답 반환
                                3. GET /api/festivals/raw
                                   → TourAPI 원본 응답 확인용 개발/디버깅 API
                                """)
                        .version("v1"));
    }
}