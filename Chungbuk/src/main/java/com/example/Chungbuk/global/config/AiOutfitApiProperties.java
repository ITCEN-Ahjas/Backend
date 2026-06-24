package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.outfit")
public class AiOutfitApiProperties {

    private String baseUrl = "http://localhost:8000";

    private String recommendPath = "/api/v1/outfits/recommend";

    private String batchRecommendPath =
            "/api/v1/outfits/recommendations";

    private String timeSlotRecommendPath =
            "/api/v1/outfits/time-slot-recommendations";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRecommendPath() {
        return recommendPath;
    }

    public void setRecommendPath(String recommendPath) {
        this.recommendPath = recommendPath;
    }

    public String getBatchRecommendPath() {
        return batchRecommendPath;
    }

    public void setBatchRecommendPath(
            String batchRecommendPath
    ) {
        this.batchRecommendPath = batchRecommendPath;
    }

    public String getTimeSlotRecommendPath() {
        return timeSlotRecommendPath;
    }

    public void setTimeSlotRecommendPath(
            String timeSlotRecommendPath
    ) {
        this.timeSlotRecommendPath = timeSlotRecommendPath;
    }

    public String getRecommendUrl() {
        return createUrl(recommendPath);
    }

    public String getBatchRecommendUrl() {
        return createUrl(batchRecommendPath);
    }

    public String getTimeSlotRecommendUrl() {
        return createUrl(timeSlotRecommendPath);
    }

    public void validateBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "AI 추천 서버 주소가 없습니다. "
                            + "ai.outfit.base-url 값을 확인하세요."
            );
        }
    }

    private String createUrl(String path) {
        return removeTrailingSlash(baseUrl)
                + addLeadingSlash(path);
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private String addLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value;
        }

        return "/" + value;
    }
}
