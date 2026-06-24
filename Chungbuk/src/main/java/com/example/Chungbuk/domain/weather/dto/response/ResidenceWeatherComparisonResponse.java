package com.example.Chungbuk.domain.weather.dto.response;

public class ResidenceWeatherComparisonResponse {

    private final String residenceCity;
    private final String residenceCountry;
    private final double residenceFeelsLikeTemperature;
    private final double targetFeelsLikeTemperature;
    private final double temperatureDifference;
    private final String message;

    public ResidenceWeatherComparisonResponse(
            String residenceCity,
            String residenceCountry,
            double residenceFeelsLikeTemperature,
            double targetFeelsLikeTemperature,
            double temperatureDifference,
            String message
    ) {
        this.residenceCity = residenceCity;
        this.residenceCountry = residenceCountry;
        this.residenceFeelsLikeTemperature = residenceFeelsLikeTemperature;
        this.targetFeelsLikeTemperature = targetFeelsLikeTemperature;
        this.temperatureDifference = temperatureDifference;
        this.message = message;
    }

    public String getResidenceCity() {
        return residenceCity;
    }

    public String getResidenceCountry() {
        return residenceCountry;
    }

    public double getResidenceFeelsLikeTemperature() {
        return residenceFeelsLikeTemperature;
    }

    public double getTargetFeelsLikeTemperature() {
        return targetFeelsLikeTemperature;
    }

    public double getTemperatureDifference() {
        return temperatureDifference;
    }

    public String getMessage() {
        return message;
    }
}
