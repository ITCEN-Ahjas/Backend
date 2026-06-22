package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
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
import java.util.List;

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
                      "name": "얇은 바람막이 / 점퍼",
                      "description": "일교차에 대비해 가볍게 걸치기 좋아요."
                    },
                    "top": {
                      "name": "긴팔 티셔츠 / 니트",
                      "description": "쌀쌀한 날씨에 보온성을 높여줘요."
                    },
                    "bottom": {
                      "name": "면바지 / 청바지",
                      "description": "오래 이동해도 활동하기 편해요."
                    },
                    "shoes": {
                      "name": "운동화 / 스니커즈",
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
                "얇은 바람막이 / 점퍼",
                response.getOutfitCards().getOuterwear().getName()
        );
        assertEquals(
                "water_bottle",
                response.getPreparationItems().get(0).getCode()
        );
    }

    @Test
    void recommend_returnsAiResponse_whenAiServerRespondsNormally() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AiOutfitApiProperties properties =
                createProperties();

        AiOutfitRecommendationResponse expectedResponse =
                createAiResponse();

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
                "얇은 바람막이 / 점퍼",
                actualResponse.getOutfitCards().getOuterwear().getName()
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

    private AiOutfitApiProperties createProperties() {
        AiOutfitApiProperties properties =
                new AiOutfitApiProperties();

        properties.setBaseUrl("http://localhost:8000");
        properties.setRecommendPath(
                "/api/v1/outfits/recommend"
        );

        return properties;
    }

    private AiOutfitRecommendationRequest createRequest() {
        return new AiOutfitRecommendationRequest(
                "청주",
                "많이 걷는 여행",
                new AiOutfitRecommendationRequest.CurrentWeather(
                        17.0,
                        72,
                        2.6,
                        "보통 바람",
                        "강수 없음",
                        "없음",
                        40,
                        "흐림",
                        "흐림"
                ),
                new AiOutfitRecommendationRequest.FeelsLikeWeather(
                        14.0,
                        -3.0,
                        "바람과 습도로 실제 기온보다 쌀쌀합니다.",
                        List.of("바람", "습도")
                )
        );
    }

    private AiOutfitRecommendationResponse createAiResponse() {
        AiOutfitRecommendationResponse.OutfitCards outfitCards =
                new AiOutfitRecommendationResponse.OutfitCards(
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "얇은 바람막이 / 점퍼",
                                "일교차에 대비해 가볍게 걸치기 좋아요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "긴팔 티셔츠 / 니트",
                                "쌀쌀한 날씨에 보온성을 높여줘요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "면바지 / 청바지",
                                "오래 이동해도 활동하기 편해요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "운동화 / 스니커즈",
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
                "많이 걷는 여행",
                "ai",
                outfitCards,
                preparationItems
        );
    }
}