package com.example.Chungbuk.domain.weather.dto.request;

public class OutfitRecommendationRequest {

    private String region;
    private String travelStyle;

    public OutfitRecommendationRequest() {
    }

    public OutfitRecommendationRequest(
            String region,
            String travelStyle
    ) {
        this.region = region;
        this.travelStyle = travelStyle;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }
}