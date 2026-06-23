package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeelsLikeWeatherServiceTest {

    private final FeelsLikeWeatherService feelsLikeWeatherService =
            new FeelsLikeWeatherService();

    @Test
    @DisplayName("낮은 기온과 강한 바람이 있으면 체감온도와 추위 안내 문장을 반환한다")
    void create_returnsColdWindFeelsLikeDescriptions() {
        CurrentWeatherResponse currentWeather =
                new CurrentWeatherResponse(
                        "제천",
                        4.0,
                        50,
                        5.0,
                        "강함",
                        "강수없음",
                        "강수 없음",
                        0,
                        "맑음",
                        "맑음"
                );

        FeelsLikeWeatherResponse response =
                feelsLikeWeatherService.create(currentWeather);

        assertTrue(response.getFeelsLikeTemperature() < 4.0);
        assertTrue(response.getTemperatureDifference() < 0);
        assertTrue(response.getFactors().contains("바람 영향"));
        assertTrue(response.getSummary().contains("더 낮게"));
        assertTrue(response.getDetail().contains("바람"));
    }

    @Test
    @DisplayName("높은 기온과 습도가 있으면 체감온도와 더위 안내 문장을 반환한다")
    void create_returnsHotHumidityFeelsLikeDescriptions() {
        CurrentWeatherResponse currentWeather =
                new CurrentWeatherResponse(
                        "청주",
                        30.0,
                        80,
                        1.0,
                        "약함",
                        "강수없음",
                        "강수 없음",
                        10,
                        "맑음",
                        "맑음"
                );

        FeelsLikeWeatherResponse response =
                feelsLikeWeatherService.create(currentWeather);

        assertTrue(response.getFeelsLikeTemperature() > 30.0);
        assertTrue(response.getTemperatureDifference() > 0);
        assertTrue(response.getFactors().contains("높은 기온과 습도"));
        assertTrue(response.getSummary().contains("더 높게"));
        assertTrue(response.getDetail().contains("습도"));
    }

    @Test
    @DisplayName("체감온도 차이가 작으면 현재 기온과 비슷하다는 안내 문장을 반환한다")
    void create_returnsSimilarTemperatureDescriptions() {
        CurrentWeatherResponse currentWeather =
                new CurrentWeatherResponse(
                        "청주",
                        23.1,
                        63,
                        1.8,
                        "약함",
                        "강수없음",
                        "강수 없음",
                        0,
                        "구름 많음",
                        "구름 많음"
                );

        FeelsLikeWeatherResponse response =
                feelsLikeWeatherService.create(currentWeather);

        assertTrue(response.getSummary().contains("비슷하게"));
        assertTrue(response.getDetail().contains("차이가 크지 않아"));
        assertTrue(response.getFactors().contains("현재 기온"));
    }
}