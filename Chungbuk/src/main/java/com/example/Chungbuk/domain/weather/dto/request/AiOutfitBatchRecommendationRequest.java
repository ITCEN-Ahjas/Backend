package com.example.Chungbuk.domain.weather.dto.request;

public class AiOutfitBatchRecommendationRequest {

    private String region;

    private AiOutfitRecommendationRequest.CurrentWeather
            currentWeather;

    private AiOutfitRecommendationRequest.FeelsLikeWeather
            feelsLikeWeather;

    public AiOutfitBatchRecommendationRequest() {
    }

    public AiOutfitBatchRecommendationRequest(
            String region,
            AiOutfitRecommendationRequest.CurrentWeather
                    currentWeather,
            AiOutfitRecommendationRequest.FeelsLikeWeather
                    feelsLikeWeather
    ) {
        this.region = region;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public AiOutfitRecommendationRequest.CurrentWeather
    getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(
            AiOutfitRecommendationRequest.CurrentWeather
                    currentWeather
    ) {
        this.currentWeather = currentWeather;
    }

    public AiOutfitRecommendationRequest.FeelsLikeWeather
    getFeelsLikeWeather() {
        return feelsLikeWeather;
    }

    public void setFeelsLikeWeather(
            AiOutfitRecommendationRequest.FeelsLikeWeather
                    feelsLikeWeather
    ) {
        this.feelsLikeWeather = feelsLikeWeather;
    }
}