package com.example.Chungbuk.domain.weather.dto.response;

public class CurrentWeatherResponse {

    private final String region;
    private final double temperature;
    private final int humidity;
    private final double windSpeed;
    private final String windStatus;
    private final String precipitationAmount;
    private final String precipitationType;
    private final int precipitationProbability;
    private final String skyStatus;
    private final String weatherCondition;

    public CurrentWeatherResponse(
            String region,
            double temperature,
            int humidity,
            double windSpeed,
            String windStatus,
            String precipitationAmount,
            String precipitationType,
            int precipitationProbability,
            String skyStatus,
            String weatherCondition
    ) {
        this.region = region;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windStatus = windStatus;
        this.precipitationAmount = precipitationAmount;
        this.precipitationType = precipitationType;
        this.precipitationProbability = precipitationProbability;
        this.skyStatus = skyStatus;
        this.weatherCondition = weatherCondition;
    }

    public String getRegion() {
        return region;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getWindStatus() {
        return windStatus;
    }

    public String getPrecipitationAmount() {
        return precipitationAmount;
    }

    public String getPrecipitationType() {
        return precipitationType;
    }

    public int getPrecipitationProbability() {
        return precipitationProbability;
    }

    public String getSkyStatus() {
        return skyStatus;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }
}