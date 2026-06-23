package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.request.AiOutfitBatchRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitBatchRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.global.config.AiOutfitApiProperties;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiOutfitRecommendationClientTest {

    @Test
    void aiResponseJson_mapsToResponseDto() throws Exception {
        String json = """
                {
                  "region": "청주",
                  "travelStyle": "많이 걷는 여행",
                  "source": "ai",
                  "outfitCards": {
                    "outerwear": {
                      "code": "windbreaker",
                      "name": "얇은 바람막이 / 점퍼",
                      "description": "일교차에 대비해 가볍게 걸치기 좋아요."
                    },
                    "top": {
                      "code": "long_sleeve_tshirt",
                      "name": "긴팔 티셔츠",
                      "description": "쌀쌀한 날씨에 보온성을 높여줘요."
                    },
                    "bottom": {
                      "code": "cotton_pants",
                      "name": "면바지",
                      "description": "오래 이동해도 활동하기 편해요."
                    },
                    "shoes": {
                      "code": "cushioned_sneakers",
                      "name": "쿠션 운동화",
                      "description": "장시간 걷기에도 발이 편안해요."
                    }
                  },
                  "preparationItems": [
                    {
                      "code": "water_bottle",
                      "name": "물병",
                      "description": "여행 중 수분 보충을 위해 챙기세요."
                    }
                  ]
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();

        AiOutfitRecommendationResponse response =
                objectMapper.readValue(
                        json,
                        AiOutfitRecommendationResponse.class
                );

        assertEquals("청주", response.getRegion());
        assertEquals("많이 걷는 여행", response.getTravelStyle());
        assertEquals("ai", response.getSource());

        assertEquals(
                "windbreaker",
                response.getOutfitCards().getOuterwear().getCode()
        );

        assertEquals(
                "water_bottle",
                response.getPreparationItems().get(0).getCode()
        );
    }

    @Test
    void batchAiResponseJson_mapsToBatchResponseDto() throws Exception {
        String json = """
                {
                  "region": "청주",
                  "source": "fallback",
                  "recommendations": {
                    "기본 추천": {
                      "region": "청주",
                      "travelStyle": "기본 추천",
                      "source": "fallback",
                      "outfitCards": {
                        "outerwear": {
                          "code": "light_jacket",
                          "name": "얇은 점퍼 / 가벼운 재킷",
                          "description": "선선한 날씨와 일교차에 대응하기 좋아요."
                        },
                        "top": {
                          "code": "long_sleeve_tshirt",
                          "name": "긴팔 티셔츠",
                          "description": "낮과 저녁 모두 편하게 입을 수 있는 기본 상의예요."
                        },
                        "bottom": {
                          "code": "jeans",
                          "name": "청바지",
                          "description": "가벼운 관광과 도심 이동에 무난하게 입기 좋아요."
                        },
                        "shoes": {
                          "code": "sneakers",
                          "name": "운동화",
                          "description": "도심 관광과 기본 이동에 무난해요."
                        }
                      },
                      "preparationItems": [
                        {
                          "code": "water_bottle",
                          "name": "물병",
                          "description": "여행 중 수분 보충을 위해 챙기세요."
                        }
                      ]
                    }
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();

        AiOutfitBatchRecommendationResponse response =
                objectMapper.readValue(
                        json,
                        AiOutfitBatchRecommendationResponse.class
                );

        assertEquals("청주", response.getRegion());
        assertEquals("fallback", response.getSource());

        AiOutfitRecommendationResponse recommendation =
                response.getRecommendations().get("기본 추천");

        assertNotNull(recommendation);

        assertEquals(
                "light_jacket",
                recommendation.getOutfitCards()
                        .getOuterwear()
                        .getCode()
        );
    }

    @Test
    void recommend_returnsAiResponse_whenAiServerRespondsNormally() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        AiOutfitApiProperties properties =
                createProperties();

        AiOutfitRecommendationResponse expectedResponse =
                createAiResponse("많이 걷는 여행");

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiOutfitRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        properties
                );

        AiOutfitRecommendationResponse actualResponse =
                client.recommend(createRequest());

        assertNotNull(actualResponse);
        assertEquals("ai", actualResponse.getSource());

        assertEquals(
                "windbreaker",
                actualResponse.getOutfitCards()
                        .getOuterwear()
                        .getCode()
        );
    }

    @Test
    void recommendBatch_returnsAiBatchResponse_whenAiServerRespondsNormally() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        AiOutfitApiProperties properties =
                createProperties();

        AiOutfitBatchRecommendationResponse expectedResponse =
                createBatchAiResponse();

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiOutfitBatchRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        properties
                );

        AiOutfitBatchRecommendationResponse actualResponse =
                client.recommendBatch(createBatchRequest());

        assertNotNull(actualResponse);
        assertEquals("fallback", actualResponse.getSource());

        assertEquals(
                6,
                actualResponse.getRecommendations().size()
        );
    }

    @Test
    void recommend_throwsException_whenAiServerConnectionFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        AiOutfitApiProperties properties =
                createProperties();

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiOutfitRecommendationResponse.class)
        )).thenThrow(new RestClientException("connection failed"));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        properties
                );

        assertThrows(
                AiOutfitApiException.class,
                () -> client.recommend(createRequest())
        );
    }

    @Test
    void recommendBatch_throwsException_whenAiServerConnectionFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        AiOutfitApiProperties properties =
                createProperties();

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiOutfitBatchRecommendationResponse.class)
        )).thenThrow(new RestClientException("connection failed"));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        properties
                );

        assertThrows(
                AiOutfitApiException.class,
                () -> client.recommendBatch(createBatchRequest())
        );
    }

    private AiOutfitApiProperties createProperties() {
        AiOutfitApiProperties properties =
                new AiOutfitApiProperties();

        properties.setBaseUrl("http://localhost:8000");

        properties.setRecommendPath(
                "/api/v1/outfits/recommend"
        );

        properties.setBatchRecommendPath(
                "/api/v1/outfits/recommendations"
        );

        return properties;
    }

    private AiOutfitRecommendationRequest createRequest() {
        return new AiOutfitRecommendationRequest(
                "청주",
                "많이 걷는 여행",
                createCurrentWeather(),
                createFeelsLikeWeather()
        );
    }

    private AiOutfitBatchRecommendationRequest createBatchRequest() {
        return new AiOutfitBatchRecommendationRequest(
                "청주",
                createCurrentWeather(),
                createFeelsLikeWeather()
        );
    }

    private AiOutfitRecommendationRequest.CurrentWeather
    createCurrentWeather() {
        return new AiOutfitRecommendationRequest.CurrentWeather(
                17.0,
                72,
                2.6,
                "보통 바람",
                "강수 없음",
                "없음",
                40,
                "흐림",
                "흐림"
        );
    }

    private AiOutfitRecommendationRequest.FeelsLikeWeather
    createFeelsLikeWeather() {
        return new AiOutfitRecommendationRequest.FeelsLikeWeather(
                14.0,
                -3.0,
                "바람과 습도로 실제 기온보다 쌀쌀합니다.",
                List.of("바람", "습도")
        );
    }

    private AiOutfitRecommendationResponse createAiResponse(
            String travelStyle
    ) {
        AiOutfitRecommendationResponse.OutfitCards outfitCards =
                new AiOutfitRecommendationResponse.OutfitCards(
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "windbreaker",
                                "얇은 바람막이 / 점퍼",
                                "일교차에 대비해 가볍게 걸치기 좋아요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "long_sleeve_tshirt",
                                "긴팔 티셔츠",
                                "쌀쌀한 날씨에 보온성을 높여줘요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "cotton_pants",
                                "면바지",
                                "오래 이동해도 활동하기 편해요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "cushioned_sneakers",
                                "쿠션 운동화",
                                "장시간 걷기에도 발이 편안해요."
                        )
                );

        List<AiOutfitRecommendationResponse.PreparationItem>
                preparationItems = List.of(
                new AiOutfitRecommendationResponse.PreparationItem(
                        "water_bottle",
                        "물병",
                        "여행 중 수분 보충을 위해 챙기세요."
                )
        );

        return new AiOutfitRecommendationResponse(
                "청주",
                travelStyle,
                "ai",
                outfitCards,
                preparationItems
        );
    }

    private AiOutfitBatchRecommendationResponse
    createBatchAiResponse() {
        Map<String, AiOutfitRecommendationResponse>
                recommendations = new LinkedHashMap<>();

        recommendations.put(
                "기본 추천",
                createAiResponse("기본 추천")
        );

        recommendations.put(
                "많이 걷는 여행",
                createAiResponse("많이 걷는 여행")
        );

        recommendations.put(
                "야외 활동",
                createAiResponse("야외 활동")
        );

        recommendations.put(
                "실내 중심",
                createAiResponse("실내 중심")
        );

        recommendations.put(
                "야간 일정",
                createAiResponse("야간 일정")
        );

        recommendations.put(
                "비 오는 날 대비",
                createAiResponse("비 오는 날 대비")
        );

        return new AiOutfitBatchRecommendationResponse(
                "청주",
                "fallback",
                recommendations
        );
    }
}