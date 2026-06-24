package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegionTimeSlotOutfitRecommendationResponse {

    private final String region;
    private final LocalDateTime updatedAt;
    private final LocalDate forecastDate;
    private final String source;

    private final List<TimeSlotOutfitRecommendationResponse>
            recommendations;

    public RegionTimeSlotOutfitRecommendationResponse(
            String region,
            LocalDateTime updatedAt,
            LocalDate forecastDate,
            String source,
            List<TimeSlotOutfitRecommendationResponse>
                    recommendations
    ) {
        this.region = region;
        this.updatedAt = updatedAt;
        this.forecastDate = forecastDate;
        this.source = source;
        this.recommendations = List.copyOf(recommendations);
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

    public String getSource() {
        return source;
    }

    public List<TimeSlotOutfitRecommendationResponse>
    getRecommendations() {
        return recommendations;
    }
}
