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
                                충청북도 축제·공연·행사·관광지·문화시설·레포츠 정보를 제공합니다.

                                조회 API는 DB에 저장된 festival_contents 데이터를 기준으로 응답합니다.
                                TourAPI는 프론트엔드 요청마다 직접 호출하지 않고,
                                초기 적재 및 자동 갱신 동기화 API를 통해 DB를 갱신하는 데이터 소스로 사용합니다.

                                주요 흐름:
                                1. POST /api/festivals/sync/bootstrap
                                   → 전체 목록, 공통 상세, 유형별 상세, 이미지 데이터를 가능한 범위까지 초기 적재

                                2. POST /api/festivals/sync/refresh
                                   → 신규, 수정, 상세 미완료 콘텐츠 중심으로 자동 갱신

                                3. GET /api/festivals
                                   GET /api/festivals/experiences
                                   GET /api/festivals/{contentId}
                                   → DB 저장 데이터를 기준으로 응답

                                레포츠 카드에서는 원본 이용시간 제공률이 낮으므로
                                이용시간 대신 장소 정보를 기본으로 표시합니다.

                                동기화 요청은 TourAPI 호출을 최대 900회로 제한합니다.
                                호출 예산이 끝나면 상세 미완료 콘텐츠는 다음 실행에서 이어서 처리됩니다.
                                """)
                        .version("v2"));
    }
}