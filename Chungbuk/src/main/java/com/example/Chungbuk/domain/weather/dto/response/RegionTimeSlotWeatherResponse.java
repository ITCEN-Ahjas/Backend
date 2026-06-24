package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegionTimeSlotWeatherResponse {

    private final String region;
    private final LocalDateTime updatedAt;
    private final LocalDate forecastDate;
    private final List<TimeSlotWeatherResponse> timeSlots;

    public RegionTimeSlotWeatherResponse(
            String region,
            LocalDateTime updatedAt,
            LocalDate forecastDate,
            List<TimeSlotWeatherResponse> timeSlots
    ) {
        this.region = region;
        this.updatedAt = updatedAt;
        this.forecastDate = forecastDate;
        this.timeSlots = List.copyOf(timeSlots);
    }

    public String getRegion() {
        return region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public List<TimeSlotWeatherResponse> getTimeSlots() {
        return timeSlots;
    }
}
