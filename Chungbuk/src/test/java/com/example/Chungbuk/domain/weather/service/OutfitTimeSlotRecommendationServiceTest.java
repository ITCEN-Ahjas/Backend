package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.AiOutfitRecommendationClient;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.request.AiTimeSlotOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.AiTimeSlotOutfitBatchRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutfitTimeSlotRecommendationServiceTest {

    @Test
    void recommendTimeSlots_returnsWeatherAndAiRecommendations() {
        WeatherService weatherService = mock(WeatherService.class);
        AiOutfitRecommendationClient aiClient =
                mock(AiOutfitRecommendationClient.class);

        OutfitRecommendationService service =
                new OutfitRecommendationService(
                        weatherService,
                        aiClient
                );

        when(weatherService.getRegionTimeSlotWeather(
                any(RegionWeatherRequest.class)
        )).thenReturn(createWeatherResponse());

        when(aiClient.recommendTimeSlots(
                any(AiTimeSlotOutfitRecommendationRequest.class)
        )).thenReturn(createAiResponse());

        RegionTimeSlotOutfitRecommendationResponse response =
                service.recommendTimeSlots("청주");

        ArgumentCaptor<AiTimeSlotOutfitRecommendationRequest>
                requestCaptor = ArgumentCaptor.forClass(
                        AiTimeSlotOutfitRecommendationRequest.class
                );

        verify(aiClient).recommendTimeSlots(requestCaptor.capture());

        AiTimeSlotOutfitRecommendationRequest aiRequest =
                requestCaptor.getValue();

        assertEquals("청주", aiRequest.getRegion());
        assertEquals(2, aiRequest.getTimeSlots().size());
        assertEquals(
                "afternoon",
                aiRequest.getTimeSlots().get(0).getTimeSlot()
        );
        assertEquals(
                26.0,
                aiRequest.getTimeSlots()
                        .get(0)
                        .getCurrentWeather()
                        .getTemperature()
        );

        assertEquals("청주", response.getRegion());
        assertEquals(LocalDate.of(2026, 6, 24), response.getForecastDate());
        assertEquals("ai", response.getSource());
        assertEquals(2, response.getRecommendations().size());
        assertEquals(
                "오후",
                response.getRecommendations().get(0).getTimeSlotName()
        );
        assertEquals(
                "얇은 셔츠 / 자외선 차단 셔츠",
                response.getRecommendations()
                        .get(0)
                        .getOutfitCards()
                        .getOuterwear()
                        .getName()
        );
    }

    @Test
    void recommendTimeSlots_throwsException_whenAiResultMissesTimeSlot() {
        WeatherService weatherService = mock(WeatherService.class);
        AiOutfitRecommendationClient aiClient =
                mock(AiOutfitRecommendationClient.class);

        OutfitRecommendationService service =
                new OutfitRecommendationService(
                        weatherService,
                        aiClient
                );

        when(weatherService.getRegionTimeSlotWeather(
                any(RegionWeatherRequest.class)
        )).thenReturn(createWeatherResponse());

        when(aiClient.recommendTimeSlots(
                any(AiTimeSlotOutfitRecommendationRequest.class)
        )).thenReturn(
                new AiTimeSlotOutfitBatchRecommendationResponse(
                        "청주",
                        "fallback",
                        List.of(createAiRecommendation("afternoon"))
                )
        );

        assertThrows(
                AiOutfitApiException.class,
                () -> service.recommendTimeSlots("청주")
        );
    }

    private RegionTimeSlotWeatherResponse createWeatherResponse() {
        CurrentWeatherResponse afternoonWeather =
                createCurrentWeather(26.0, 55, 4.1, 30, "흐림");

        CurrentWeatherResponse eveningWeather =
                createCurrentWeather(23.0, 70, 3.0, 20, "구름 많음");

        FeelsLikeWeatherResponse afternoonFeelsLike =
                createFeelsLikeWeather(26.0);

        FeelsLikeWeatherResponse eveningFeelsLike =
                createFeelsLikeWeather(23.0);

        TimeSlotWeatherResponse afternoon =
                new TimeSlotWeatherResponse(
                        WeatherTimeSlot.AFTERNOON,
                        LocalDateTime.of(2026, 6, 24, 15, 0),
                        afternoonWeather,
                        afternoonFeelsLike
                );

        TimeSlotWeatherResponse evening =
                new TimeSlotWeatherResponse(
                        WeatherTimeSlot.EVENING,
                        LocalDateTime.of(2026, 6, 24, 19, 0),
                        eveningWeather,
                        eveningFeelsLike
                );

        return new RegionTimeSlotWeatherResponse(
                "청주",
                LocalDateTime.of(2026, 6, 24, 14, 30),
                LocalDate.of(2026, 6, 24),
                List.of(afternoon, evening)
        );
    }

    private CurrentWeatherResponse createCurrentWeather(
            double temperature,
            int humidity,
            double windSpeed,
            int precipitationProbability,
            String weatherCondition
    ) {
        return new CurrentWeatherResponse(
                "청주",
                temperature,
                humidity,
                windSpeed,
                "보통",
                "강수 없음",
                "강수 없음",
                precipitationProbability,
                weatherCondition,
                weatherCondition
        );
    }

    private FeelsLikeWeatherResponse createFeelsLikeWeather(
            double temperature
    ) {
        return new FeelsLikeWeatherResponse(
                temperature,
                0.0,
                "현재 기온과 비슷하게 느껴집니다.",
                "현재 기온과 비슷해요.",
                "기온과 체감온도 차이가 크지 않습니다.",
                List.of("현재 기온")
        );
    }

    private AiTimeSlotOutfitBatchRecommendationResponse createAiResponse() {
        return new AiTimeSlotOutfitBatchRecommendationResponse(
                "청주",
                "ai",
                List.of(
                        createAiRecommendation("afternoon"),
                        createAiRecommendation("evening")
                )
        );
    }

    private AiTimeSlotOutfitBatchRecommendationResponse
            .TimeSlotOutfitRecommendation
    createAiRecommendation(String timeSlot) {
        boolean isAfternoon = "afternoon".equals(timeSlot);

        AiOutfitRecommendationResponse.OutfitCards outfitCards =
                new AiOutfitRecommendationResponse.OutfitCards(
                        new AiOutfitRecommendationResponse.OutfitCard(
                                isAfternoon
                                        ? "uv_shirt"
                                        : "light_jacket",
                                isAfternoon
                                        ? "얇은 셔츠 / 자외선 차단 셔츠"
                                        : "얇은 점퍼 / 가벼운 재킷",
                                "시간대 날씨에 맞춘 겉옷입니다."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "short_sleeve_tshirt",
                                "반소매 티셔츠",
                                "통기성이 좋아 편안하게 입을 수 있어요."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "lightweight_pants",
                                "얇은 긴바지",
                                "여행 중 움직이기 편안합니다."
                        ),
                        new AiOutfitRecommendationResponse.OutfitCard(
                                "sneakers",
                                "운동화",
                                "관광지 이동에 편안합니다."
                        )
                );

        return new AiTimeSlotOutfitBatchRecommendationResponse
                .TimeSlotOutfitRecommendation(
                        timeSlot,
                        isAfternoon ? "오후" : "저녁",
                        LocalDateTime.of(
                                2026,
                                6,
                                24,
                                isAfternoon ? 15 : 19,
                                0
                        ),
                        isAfternoon
                                ? WeatherTimeSlot.AFTERNOON.getStartTime()
                                : WeatherTimeSlot.EVENING.getStartTime(),
                        isAfternoon
                                ? WeatherTimeSlot.AFTERNOON.getEndTime()
                                : WeatherTimeSlot.EVENING.getEndTime(),
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
    }
}
