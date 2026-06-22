package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class RegionOutfitRecommendationResponse {

    private String region;
    private LocalDateTime updatedAt;
    private String travelStyle;
    private CurrentWeatherResponse currentWeather;
    private FeelsLikeWeatherResponse feelsLikeWeather;
    private AiOutfitRecommendationResponse.OutfitCards outfitCards;
    private List<AiOutfitRecommendationResponse.PreparationItem>
            preparationItems;

    public RegionOutfitRecommendationResponse() {
    }

    public RegionOutfitRecommendationResponse(
            String region,
            LocalDateTime updatedAt,
            String travelStyle,
            CurrentWeatherResponse currentWeather,
            FeelsLikeWeatherResponse feelsLikeWeather,
            AiOutfitRecommendationResponse.OutfitCards outfitCards,
            List<AiOutfitRecommendationResponse.PreparationItem>
                    preparationItems
    ) {
        this.region = region;
        this.updatedAt = updatedAt;
        this.travelStyle = travelStyle;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
        this.outfitCards = outfitCards;
        this.preparationItems = preparationItems;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public CurrentWeatherResponse getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(
            CurrentWeatherResponse currentWeather
    ) {
        this.currentWeather = currentWeather;
    }

    public FeelsLikeWeatherResponse getFeelsLikeWeather() {
        return feelsLikeWeather;
    }

    public void setFeelsLikeWeather(
            FeelsLikeWeatherResponse feelsLikeWeather
    ) {
        this.feelsLikeWeather = feelsLikeWeather;
    }

    public AiOutfitRecommendationResponse.OutfitCards
    getOutfitCards() {
        return outfitCards;
    }

    public void setOutfitCards(
            AiOutfitRecommendationResponse.OutfitCards outfitCards
    ) {
        this.outfitCards = outfitCards;
    }

    public List<AiOutfitRecommendationResponse.PreparationItem>
    getPreparationItems() {
        return preparationItems;
    }

    public void setPreparationItems(
            List<AiOutfitRecommendationResponse.PreparationItem>
                    preparationItems
    ) {
        this.preparationItems = preparationItems;
    }
}