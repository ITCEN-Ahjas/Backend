package com.example.Chungbuk.domain.weather.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AiTimeSlotOutfitRecommendationRequest {

    private String region;
    private List<TimeSlotWeather> timeSlots;

    public AiTimeSlotOutfitRecommendationRequest() {
    }

    public AiTimeSlotOutfitRecommendationRequest(
            String region,
            List<TimeSlotWeather> timeSlots
    ) {
        this.region = region;
        this.timeSlots = List.copyOf(timeSlots);
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<TimeSlotWeather> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlotWeather> timeSlots) {
        this.timeSlots = timeSlots;
    }

    public static class TimeSlotWeather {

        private String timeSlot;
        private String timeSlotName;
        private LocalDateTime forecastAt;
        private LocalTime startTime;
        private LocalTime endTime;

        private AiOutfitRecommendationRequest.CurrentWeather
                currentWeather;

        private AiOutfitRecommendationRequest.FeelsLikeWeather
                feelsLikeWeather;

        public TimeSlotWeather() {
        }

        public TimeSlotWeather(
                String timeSlot,
                String timeSlotName,
                LocalDateTime forecastAt,
                LocalTime startTime,
                LocalTime endTime,
                AiOutfitRecommendationRequest.CurrentWeather
                        currentWeather,
                AiOutfitRecommendationRequest.FeelsLikeWeather
                        feelsLikeWeather
        ) {
            this.timeSlot = timeSlot;
            this.timeSlotName = timeSlotName;
            this.forecastAt = forecastAt;
            this.startTime = startTime;
            this.endTime = endTime;
            this.currentWeather = currentWeather;
            this.feelsLikeWeather = feelsLikeWeather;
        }

        public String getTimeSlot() {
            return timeSlot;
        }

        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }

        public String getTimeSlotName() {
            return timeSlotName;
        }

        public void setTimeSlotName(String timeSlotName) {
            this.timeSlotName = timeSlotName;
        }

        public LocalDateTime getForecastAt() {
            return forecastAt;
        }

        public void setForecastAt(LocalDateTime forecastAt) {
            this.forecastAt = forecastAt;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public AiOutfitRecommendationRequest.CurrentWeather
        getCurrentWeather() {
            return currentWeather;
        }

        public void setCurrentWeather(
                AiOutfitRecommendationRequest.CurrentWeather
                        currentWeather
        ) {
            this.currentWeather = currentWeather;
        }

        public AiOutfitRecommendationRequest.FeelsLikeWeather
        getFeelsLikeWeather() {
            return feelsLikeWeather;
        }

        public void setFeelsLikeWeather(
                AiOutfitRecommendationRequest.FeelsLikeWeather
                        feelsLikeWeather
        ) {
            this.feelsLikeWeather = feelsLikeWeather;
        }
    }
}
