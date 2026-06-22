package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.outfit")
public class AiOutfitApiProperties {

    private String baseUrl = "http://localhost:8000";
    private String recommendPath = "/api/v1/outfits/recommend";

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

    public String getRecommendUrl() {
        String normalizedBaseUrl = removeTrailingSlash(baseUrl);
        String normalizedRecommendPath = addLeadingSlash(recommendPath);

        return normalizedBaseUrl + normalizedRecommendPath;
    }

    public void validateBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "AI 추천 서버 주소가 없습니다. "
                            + "ai.outfit.base-url 값을 확인하세요."
            );
        }
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