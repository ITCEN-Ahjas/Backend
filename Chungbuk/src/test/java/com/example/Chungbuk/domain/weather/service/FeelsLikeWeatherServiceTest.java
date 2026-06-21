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
    @DisplayName("낮은 기온과 강한 바람이 있으면 실제 기온보다 낮은 체감온도를 반환한다")
    void create_returnsLowerFeelsLikeTemperatureInColdWind() {
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
    }

    @Test
    @DisplayName("높은 기온과 습도가 있으면 실제 기온보다 높은 체감온도를 반환한다")
    void create_returnsHigherFeelsLikeTemperatureInHotHumidity() {
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
    }
}