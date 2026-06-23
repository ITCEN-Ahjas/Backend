package com.example.Chungbuk.domain.weather.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiOutfitBatchRecommendationResponse {

    private String region;
    private String source;

    private Map<String, AiOutfitRecommendationResponse>
            recommendations;

    public AiOutfitBatchRecommendationResponse() {
    }

    public AiOutfitBatchRecommendationResponse(
            String region,
            String source,
            Map<String, AiOutfitRecommendationResponse>
                    recommendations
    ) {
        this.region = region;
        this.source = source;
        this.recommendations = recommendations;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, AiOutfitRecommendationResponse>
    getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(
            Map<String, AiOutfitRecommendationResponse>
                    recommendations
    ) {
        this.recommendations = recommendations;
    }
}