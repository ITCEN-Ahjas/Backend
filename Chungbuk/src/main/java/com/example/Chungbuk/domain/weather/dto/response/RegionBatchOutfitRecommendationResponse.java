package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegionBatchOutfitRecommendationResponse {

    private final String region;
    private final LocalDateTime updatedAt;
    private final String source;

    private final CurrentWeatherResponse currentWeather;

    private final FeelsLikeWeatherResponse feelsLikeWeather;

    private final Map<String, AiOutfitRecommendationResponse>
            recommendations;

    public RegionBatchOutfitRecommendationResponse(
            String region,
            LocalDateTime updatedAt,
            String source,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather,
            Map<String, AiOutfitRecommendationResponse>
                    recommendations
    ) {
        this.region = region;
        this.updatedAt = updatedAt;
        this.source = source;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
        this.recommendations = Map.copyOf(
                new LinkedHashMap<>(recommendations)
        );
    }

    public String getRegion() {
        return region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getSource() {
        return source;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }

    public FeelsLikeWeatherResponse getFeelsLikeWeather() {
        return feelsLikeWeather;
    }

    public Map<String, AiOutfitRecommendationResponse>
    getRecommendations() {
        return recommendations;
    }
}