package com.example.Chungbuk.domain.weather.dto.response;

import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeSlotWeatherResponse {

    private final String timeSlot;
    private final String timeSlotName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final LocalDateTime forecastAt;
    private final CurrentWeatherResponse currentWeather;
    private final FeelsLikeWeatherResponse feelsLikeWeather;

    public TimeSlotWeatherResponse(
            WeatherTimeSlot weatherTimeSlot,
            LocalDateTime forecastAt,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather
    ) {
        this.timeSlot = weatherTimeSlot.getCode();
        this.timeSlotName = weatherTimeSlot.getDisplayName();
        this.startTime = weatherTimeSlot.getStartTime();
        this.endTime = weatherTimeSlot.getEndTime();
        this.forecastAt = forecastAt;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public String getTimeSlotName() {
        return timeSlotName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }

    public FeelsLikeWeatherResponse getFeelsLikeWeather() {
        return feelsLikeWeather;
    }
}
