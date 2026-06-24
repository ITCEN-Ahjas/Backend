package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.open-meteo")
public class OpenMeteoApiProperties {

    private String forecastBaseUrl = "https://api.open-meteo.com/v1";

    private String geocodingBaseUrl =
            "https://geocoding-api.open-meteo.com/v1";

    public String getForecastBaseUrl() {
        return forecastBaseUrl;
    }

    public void setForecastBaseUrl(String forecastBaseUrl) {
        this.forecastBaseUrl = forecastBaseUrl;
    }

    public String getGeocodingBaseUrl() {
        return geocodingBaseUrl;
    }

    public void setGeocodingBaseUrl(String geocodingBaseUrl) {
        this.geocodingBaseUrl = geocodingBaseUrl;
    }

    public String getForecastUrl() {
        return removeTrailingSlash(forecastBaseUrl) + "/forecast";
    }

    public String getGeocodingSearchUrl() {
        return removeTrailingSlash(geocodingBaseUrl) + "/search";
    }

    public void validateUrls() {
        if (isBlank(forecastBaseUrl) || isBlank(geocodingBaseUrl)) {
            throw new IllegalStateException(
                    "거주 도시 날씨 API 주소가 없습니다. "
                            + "weather.open-meteo 설정을 확인하세요."
            );
        }
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
