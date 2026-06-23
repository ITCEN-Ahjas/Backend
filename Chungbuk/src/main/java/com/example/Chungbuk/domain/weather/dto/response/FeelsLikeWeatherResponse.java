package com.example.Chungbuk.domain.weather.dto.response;

import java.util.List;

public class FeelsLikeWeatherResponse {

    private final double feelsLikeTemperature;
    private final double temperatureDifference;
    private final String description;
    private final String summary;
    private final String detail;
    private final List<String> factors;

    public FeelsLikeWeatherResponse(
            double feelsLikeTemperature,
            double temperatureDifference,
            String description,
            String summary,
            String detail,
            List<String> factors
    ) {
        this.feelsLikeTemperature = feelsLikeTemperature;
        this.temperatureDifference = temperatureDifference;
        this.description = description;
        this.summary = summary;
        this.detail = detail;
        this.factors = List.copyOf(factors);
    }

    public double getFeelsLikeTemperature() {
        return feelsLikeTemperature;
    }

    public double getTemperatureDifference() {
        return temperatureDifference;
    }

    public String getDescription() {
        return description;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public List<String> getFactors() {
        return factors;
    }
}