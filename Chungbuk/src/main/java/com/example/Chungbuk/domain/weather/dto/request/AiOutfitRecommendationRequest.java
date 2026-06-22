package com.example.Chungbuk.domain.weather.dto.request;

import java.util.List;

public class AiOutfitRecommendationRequest {

    private String region;
    private String travelStyle;
    private CurrentWeather currentWeather;
    private FeelsLikeWeather feelsLikeWeather;

    public AiOutfitRecommendationRequest() {
    }

    public AiOutfitRecommendationRequest(
            String region,
            String travelStyle,
            CurrentWeather currentWeather,
            FeelsLikeWeather feelsLikeWeather
    ) {
        this.region = region;
        this.travelStyle = travelStyle;
        this.currentWeather = currentWeather;
        this.feelsLikeWeather = feelsLikeWeather;
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

    public CurrentWeather getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(CurrentWeather currentWeather) {
        this.currentWeather = currentWeather;
    }

    public FeelsLikeWeather getFeelsLikeWeather() {
        return feelsLikeWeather;
    }

    public void setFeelsLikeWeather(FeelsLikeWeather feelsLikeWeather) {
        this.feelsLikeWeather = feelsLikeWeather;
    }

    public static class CurrentWeather {

        private Double temperature;
        private Integer humidity;
        private Double windSpeed;
        private String windStatus;
        private String precipitationAmount;
        private String precipitationType;
        private Integer precipitationProbability;
        private String skyStatus;
        private String weatherCondition;

        public CurrentWeather() {
        }

        public CurrentWeather(
                Double temperature,
                Integer humidity,
                Double windSpeed,
                String windStatus,
                String precipitationAmount,
                String precipitationType,
                Integer precipitationProbability,
                String skyStatus,
                String weatherCondition
        ) {
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

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getHumidity() {
            return humidity;
        }

        public void setHumidity(Integer humidity) {
            this.humidity = humidity;
        }

        public Double getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(Double windSpeed) {
            this.windSpeed = windSpeed;
        }

        public String getWindStatus() {
            return windStatus;
        }

        public void setWindStatus(String windStatus) {
            this.windStatus = windStatus;
        }

        public String getPrecipitationAmount() {
            return precipitationAmount;
        }

        public void setPrecipitationAmount(String precipitationAmount) {
            this.precipitationAmount = precipitationAmount;
        }

        public String getPrecipitationType() {
            return precipitationType;
        }

        public void setPrecipitationType(String precipitationType) {
            this.precipitationType = precipitationType;
        }

        public Integer getPrecipitationProbability() {
            return precipitationProbability;
        }

        public void setPrecipitationProbability(
                Integer precipitationProbability
        ) {
            this.precipitationProbability = precipitationProbability;
        }

        public String getSkyStatus() {
            return skyStatus;
        }

        public void setSkyStatus(String skyStatus) {
            this.skyStatus = skyStatus;
        }

        public String getWeatherCondition() {
            return weatherCondition;
        }

        public void setWeatherCondition(String weatherCondition) {
            this.weatherCondition = weatherCondition;
        }
    }

    public static class FeelsLikeWeather {

        private Double feelsLikeTemperature;
        private Double temperatureDifference;
        private String description;
        private List<String> factors;

        public FeelsLikeWeather() {
        }

        public FeelsLikeWeather(
                Double feelsLikeTemperature,
                Double temperatureDifference,
                String description,
                List<String> factors
        ) {
            this.feelsLikeTemperature = feelsLikeTemperature;
            this.temperatureDifference = temperatureDifference;
            this.description = description;
            this.factors = factors;
        }

        public Double getFeelsLikeTemperature() {
            return feelsLikeTemperature;
        }

        public void setFeelsLikeTemperature(
                Double feelsLikeTemperature
        ) {
            this.feelsLikeTemperature = feelsLikeTemperature;
        }

        public Double getTemperatureDifference() {
            return temperatureDifference;
        }

        public void setTemperatureDifference(
                Double temperatureDifference
        ) {
            this.temperatureDifference = temperatureDifference;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getFactors() {
            return factors;
        }

        public void setFactors(List<String> factors) {
            this.factors = factors;
        }
    }
}