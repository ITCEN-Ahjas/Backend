package com.example.Chungbuk.domain.main.dto.response;

import java.util.List;

public record MainSummaryResponse(
        HeroResponse hero,
        List<PopularRegionResponse> popularRegions,
        List<KeywordResponse> keywords,
        List<TodayStatResponse> todayStats,
        WeatherResponse weather,
        List<FeatureCardResponse> featureCards
) {

    public record HeroResponse(
            String title,
            String highlightText,
            String description,
            String imageUrl
    ) {
    }

    public record PopularRegionResponse(
            String id,
            String name,
            String description,
            long placeCount,
            String href,
            String imageUrl
    ) {
    }

    public record KeywordResponse(
            String id,
            String label,
            String keyword,
            String href
    ) {
    }

    public record TodayStatResponse(
            String id,
            String label,
            long value,
            String unit,
            String href
    ) {
    }

    public record WeatherResponse(
            String primaryRegion,
            String temperature,
            String condition,
            String feelsLike,
            String precipitationProbability,
            String humidity,
            String wind,
            String recommendation,
            String href,
            List<WeatherRegionResponse> regions
    ) {
    }

    public record WeatherRegionResponse(
            String id,
            String region,
            String temperature,
            String condition,
            String recommendation,
            String href
    ) {
    }

    public record FeatureCardResponse(
            String id,
            String label,
            String title,
            String description,
            String href,
            String imageUrl
    ) {
    }
}
