package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.AiTimeSlotOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.AiTimeSlotOutfitBatchRecommendationResponse;
import com.example.Chungbuk.global.config.AiOutfitApiProperties;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTimeSlotOutfitRecommendationClientTest {

    @Test
    void recommendTimeSlots_callsTimeSlotEndpoint() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiTimeSlotOutfitBatchRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(createAiResponse()));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        createProperties()
                );

        AiTimeSlotOutfitBatchRecommendationResponse response =
                client.recommendTimeSlots(createRequest());

        ArgumentCaptor<URI> uriCaptor =
                ArgumentCaptor.forClass(URI.class);

        verify(restTemplate).exchange(
                uriCaptor.capture(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiTimeSlotOutfitBatchRecommendationResponse.class)
        );

        assertEquals(
                "http://localhost:8000/api/v1/outfits/"
                        + "time-slot-recommendations",
                uriCaptor.getValue().toString()
        );

        assertNotNull(response);
        assertEquals("fallback", response.getSource());
        assertEquals(1, response.getRecommendations().size());
    }

    @Test
    void recommendTimeSlots_throwsException_whenAiServerFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiTimeSlotOutfitBatchRecommendationResponse.class)
        )).thenThrow(new RestClientException("connection failed"));

        AiOutfitRecommendationClient client =
                new AiOutfitRecommendationClient(
                        restTemplate,
                        createProperties()
                );

        assertThrows(
                AiOutfitApiException.class,
                () -> client.recommendTimeSlots(createRequest())
        );
    }

    private AiOutfitApiProperties createProperties() {
        AiOutfitApiProperties properties =
                new AiOutfitApiProperties();

        properties.setBaseUrl("http://localhost:8000/");
        return properties;
    }

    private AiTimeSlotOutfitRecommendationRequest createRequest() {
        AiOutfitRecommendationRequest.CurrentWeather currentWeather =
                new AiOutfitRecommendationRequest.CurrentWeather(
                        26.0,
                        55,
                        4.1,
                        "보통",
                        "강수 없음",
                        "강수 없음",
                        30,
                        "흐림",
                        "흐림"
                );

        AiOutfitRecommendationRequest.FeelsLikeWeather feelsLikeWeather =
                new AiOutfitRecommendationRequest.FeelsLikeWeather(
                        26.0,
                        0.0,
                        "현재 기온과 비슷하게 느껴집니다.",
                        List.of("현재 기온")
                );

        AiTimeSlotOutfitRecommendationRequest.TimeSlotWeather timeSlot =
                new AiTimeSlotOutfitRecommendationRequest.TimeSlotWeather(
                        "afternoon",
                        "오후",
                        LocalDateTime.of(2026, 6, 24, 15, 0),
                        LocalTime.of(14, 0),
                        LocalTime.of(17, 0),
                        currentWeather,
                        feelsLikeWeather
                );

        return new AiTimeSlotOutfitRecommendationRequest(
                "청주",
                List.of(timeSlot)
        );
    }

    private AiTimeSlotOutfitBatchRecommendationResponse
    createAiResponse() {
        AiOutfitRecommendationResponse.OutfitCards outfitCards =
                new AiOutfitRecommendationResponse.OutfitCards(
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "uv_shirt",
                                "얇은 셔츠 / 자외선 차단 셔츠",
                                "강한 햇빛과 실내 냉방에 가볍게 대응하기 좋아요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "short_sleeve_tshirt",
                                "반소매 티셔츠",
                                "더운 날씨에 통기성이 좋아 편안하게 입을 수 있어요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "lightweight_pants",
                                "얇은 긴바지",
                                "햇빛과 실내 냉방을 함께 대비하기 좋아요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "breathable_sneakers",
                                "통풍 좋은 운동화",
                                "더운 날 오래 걸을 때 발의 답답함을 줄여줘요."
                        )
                );

        AiTimeSlotOutfitBatchRecommendationResponse
                .TimeSlotOutfitRecommendation recommendation =
                new AiTimeSlotOutfitBatchRecommendationResponse
                        .TimeSlotOutfitRecommendation(
                                "afternoon",
                                "오후",
                                LocalDateTime.of(
                                        2026,
                                        6,
                                        24,
                                        15,
                                        0
                                ),
                                LocalTime.of(14, 0),
                                LocalTime.of(17, 0),
                                outfitCards,
                                List.of(
                                        new AiOutfitRecommendationResponse
                                                .PreparationItem(
                                                        "water_bottle",
                                                        "물병",
                                                        "관광지 이동 중 수분을 보충할 수 있어요."
                                                )
                                )
                        );

        return new AiTimeSlotOutfitBatchRecommendationResponse(
                "청주",
                "fallback",
                List.of(recommendation)
        );
    }
}
