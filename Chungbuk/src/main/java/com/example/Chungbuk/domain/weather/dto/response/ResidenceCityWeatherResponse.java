package com.example.Chungbuk.domain.weather.dto.response;

import java.time.LocalDateTime;

public class ResidenceCityWeatherResponse {

    private final String city;
    private final String country;
    private final String countryCode;
    private final String admin1;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime observedAt;
    private final double temperature;
    private final double feelsLikeTemperature;
    private final String weatherCondition;

    public ResidenceCityWeatherResponse(
            String city,
            String country,
            String countryCode,
            String admin1,
            double latitude,
            double longitude,
            LocalDateTime observedAt,
            double temperature,
            double feelsLikeTemperature,
            String weatherCondition
    ) {
        this.city = city;
        this.country = country;
        this.countryCode = countryCode;
        this.admin1 = admin1;
        this.latitude = latitude;
        this.longitude = longitude;
        this.observedAt = observedAt;
        this.temperature = temperature;
        this.feelsLikeTemperature = feelsLikeTemperature;
        this.weatherCondition = weatherCondition;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getAdmin1() {
        return admin1;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLikeTemperature() {
        return feelsLikeTemperature;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }
}
