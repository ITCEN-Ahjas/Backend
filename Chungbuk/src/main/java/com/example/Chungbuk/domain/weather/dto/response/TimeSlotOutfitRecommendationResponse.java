package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TimeSlotOutfitRecommendationResponse {

    private final String timeSlot;
    private final String timeSlotName;
    private final LocalDateTime forecastAt;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private final CurrentWeatherResponse currentWeather;
    private final FeelsLikeWeatherResponse feelsLikeWeather;

    private final ResidenceWeatherComparisonResponse
            residenceComparison;

    private final AiOutfitRecommendationResponse.OutfitCards outfitCards;

    private final List<AiOutfitRecommendationResponse.PreparationItem>
            preparationItems;

    public TimeSlotOutfitRecommendationResponse(
            String timeSlot,
            String timeSlotName,
            LocalDateTime forecastAt,
            LocalTime startTime,
            LocalTime endTime,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather,
            AiOutfitRecommendationResponse.OutfitCards outfitCards,
            List<AiOutfitRecommendationResponse.PreparationItem>
                    preparationItems
    ) {
        this(
                timeSlot,
                timeSlotName,
                forecastAt,
                startTime,
                endTime,
                currentWeather,
                feelsLikeWeather,
                null,
                outfitCards,
                preparationItems
        );
    }

    public TimeSlotOutfitRecommendationResponse(
            String timeSlot,
            String timeSlotName,
            LocalDateTime forecastAt,
            LocalTime startTime,
            LocalTime endTime,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather,
            ResidenceWeatherComparisonResponse residenceComparison,
            AiOutfitRecommendationResponse.OutfitCards outfitCards,
            List<AiOutfitRecommendationResponse.PreparationItem>
                    preparationItems
    ) {
        this.timeSlot = timeSlot;
        this.timeSlotName = timeSlotName;
        this.forecastAt = forecastAt;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
        this.residenceComparison = residenceComparison;
        this.outfitCards = outfitCards;
        this.preparationItems = List.copyOf(preparationItems);
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public String getTimeSlotName() {
        return timeSlotName;
    }

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }

    public FeelsLikeWeatherResponse getFeelsLikeWeather() {
        return feelsLikeWeather;
    }

    public ResidenceWeatherComparisonResponse
    getResidenceComparison() {
        return residenceComparison;
    }

    public AiOutfitRecommendationResponse.OutfitCards getOutfitCards() {
        return outfitCards;
    }

    public List<AiOutfitRecommendationResponse.PreparationItem>
    getPreparationItems() {
        return preparationItems;
    }
}
