package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.AiOutfitRecommendationClient;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.OutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutfitRecommendationServiceTest {

    @Test
    void recommend_returnsWeatherAndOutfitRecommendation() {
        WeatherService weatherService = mock(WeatherService.class);

        AiOutfitRecommendationClient aiClient =
                mock(AiOutfitRecommendationClient.class);

        OutfitRecommendationService service =
                new OutfitRecommendationService(
                        weatherService,
                        aiClient
                );

        WeatherPageResponse weatherPageResponse =
                createWeatherPageResponse();

        AiOutfitRecommendationResponse aiResponse =
                createAiResponse();

        when(weatherService.getRegionWeather(
                any(RegionWeatherRequest.class)
        )).thenReturn(weatherPageResponse);

        when(aiClient.recommend(
                any(AiOutfitRecommendationRequest.class)
        )).thenReturn(aiResponse);

        RegionOutfitRecommendationResponse response =
                service.recommend(
                        new OutfitRecommendationRequest(
                                "청주",
                                "많이 걷는 여행"
                        )
                );

        ArgumentCaptor<AiOutfitRecommendationRequest>
                requestCaptor =
                ArgumentCaptor.forClass(
                        AiOutfitRecommendationRequest.class
                );

        verify(aiClient).recommend(requestCaptor.capture());

        AiOutfitRecommendationRequest aiRequest =
                requestCaptor.getValue();

        assertEquals("청주", response.getRegion());

        assertEquals(
                "많이 걷는 여행",
                response.getTravelStyle()
        );

        assertEquals(
                "얇은 바람막이 / 점퍼",
                response.getOutfitCards()
                        .getOuterwear()
                        .getName()
        );

        assertEquals(
                "water_bottle",
                response.getPreparationItems()
                        .get(0)
                        .getCode()
        );

        assertEquals("청주", aiRequest.getRegion());

        assertEquals(
                "많이 걷는 여행",
                aiRequest.getTravelStyle()
        );

        assertEquals(
                17.0,
                aiRequest.getCurrentWeather().getTemperature()
        );

        assertEquals(
                14.0,
                aiRequest.getFeelsLikeWeather()
                        .getFeelsLikeTemperature()
        );
    }

    @Test
    void recommend_throwsInvalidRequestException_whenTravelStyleIsInvalid() {
        WeatherService weatherService = mock(WeatherService.class);

        AiOutfitRecommendationClient aiClient =
                mock(AiOutfitRecommendationClient.class);

        OutfitRecommendationService service =
                new OutfitRecommendationService(
                        weatherService,
                        aiClient
                );

        assertThrows(
                InvalidRequestException.class,
                () -> service.recommend(
                        new OutfitRecommendationRequest(
                                "청주",
                                "드라이브"
                        )
                )
        );

        verifyNoInteractions(weatherService, aiClient);
    }

    @Test
    void recommend_propagatesException_whenAiServerFails() {
        WeatherService weatherService = mock(WeatherService.class);

        AiOutfitRecommendationClient aiClient =
                mock(AiOutfitRecommendationClient.class);

        OutfitRecommendationService service =
                new OutfitRecommendationService(
                        weatherService,
                        aiClient
                );

        WeatherPageResponse weatherPageResponse =
                createWeatherPageResponse();

        when(weatherService.getRegionWeather(
                any(RegionWeatherRequest.class)
        )).thenReturn(weatherPageResponse);

        when(aiClient.recommend(
                any(AiOutfitRecommendationRequest.class)
        )).thenThrow(
                new AiOutfitApiException(
                        "AI 서버 연결 실패"
                )
        );

        assertThrows(
                AiOutfitApiException.class,
                () -> service.recommend(
                        new OutfitRecommendationRequest(
                                "청주",
                                "야외 활동"
                        )
                )
        );
    }

    private WeatherPageResponse createWeatherPageResponse() {
        WeatherPageResponse weatherPageResponse =
                mock(WeatherPageResponse.class);

        CurrentWeatherResponse currentWeather =
                mock(CurrentWeatherResponse.class);

        FeelsLikeWeatherResponse feelsLikeWeather =
                mock(FeelsLikeWeatherResponse.class);

        when(weatherPageResponse.getRegion()).thenReturn("청주");

        when(weatherPageResponse.getUpdatedAt()).thenReturn(
                LocalDateTime.of(
                        2026,
                        6,
                        22,
                        14,
                        30
                )
        );

        when(weatherPageResponse.getCurrentWeather())
                .thenReturn(currentWeather);

        when(weatherPageResponse.getFeelsLikeWeather())
                .thenReturn(feelsLikeWeather);

        when(currentWeather.getTemperature()).thenReturn(17.0);
        when(currentWeather.getHumidity()).thenReturn(72);
        when(currentWeather.getWindSpeed()).thenReturn(2.6);
        when(currentWeather.getWindStatus()).thenReturn("보통 바람");

        when(currentWeather.getPrecipitationAmount())
                .thenReturn("강수 없음");

        when(currentWeather.getPrecipitationType()).thenReturn("없음");

        when(currentWeather.getPrecipitationProbability())
                .thenReturn(40);

        when(currentWeather.getSkyStatus()).thenReturn("흐림");

        when(currentWeather.getWeatherCondition()).thenReturn("흐림");

        when(feelsLikeWeather.getFeelsLikeTemperature())
                .thenReturn(14.0);

        when(feelsLikeWeather.getTemperatureDifference())
                .thenReturn(-3.0);

        when(feelsLikeWeather.getDescription())
                .thenReturn(
                        "바람과 습도로 실제 기온보다 쌀쌀합니다."
                );

        when(feelsLikeWeather.getFactors())
                .thenReturn(List.of("바람", "습도"));

        return weatherPageResponse;
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