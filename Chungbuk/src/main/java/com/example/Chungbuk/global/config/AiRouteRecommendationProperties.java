package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.route")
public class AiRouteRecommendationProperties {

    private String baseUrl = "http://localhost:8000";
    private String routesPath = "/api/v1/recommend/routes";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRoutesPath() {
        return routesPath;
    }

    public void setRoutesPath(String routesPath) {
        this.routesPath = routesPath;
    }

    public String getRoutesUrl() {
        return createUrl(routesPath);
    }

    public void validateBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "AI route recommendation server URL is missing. "
                            + "Check ai.route.base-url."
            );
        }
    }

    private String createUrl(String path) {
        return removeTrailingSlash(baseUrl) + addLeadingSlash(path);
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
