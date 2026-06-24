package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;

public class ForecastWeatherSnapshot {

    private final LocalDateTime forecastAt;
    private final CurrentWeatherResponse currentWeather;

    public ForecastWeatherSnapshot(
            LocalDateTime forecastAt,
            CurrentWeatherResponse currentWeather
    ) {
        this.forecastAt = forecastAt;
        this.currentWeather = currentWeather;
    }

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }
}
