package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;

public class WeatherPageResponse {

    private final String region;
    private final LocalDateTime updatedAt;
    private final CurrentWeatherResponse currentWeather;
    private final FeelsLikeWeatherResponse feelsLikeWeather;

    public WeatherPageResponse(
            String region,
            LocalDateTime updatedAt,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather
    ) {
        this.region = region;
        this.updatedAt = updatedAt;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
    }

    public String getRegion() {
        return region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }

    public FeelsLikeWeatherResponse getFeelsLikeWeather() {
        return feelsLikeWeather;
    }
}