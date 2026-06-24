package com.example.Chungbuk.domain.weather.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiTimeSlotOutfitBatchRecommendationResponse {

    private String region;
    private String source;

    private List<TimeSlotOutfitRecommendation> recommendations;

    public AiTimeSlotOutfitBatchRecommendationResponse() {
    }

    public AiTimeSlotOutfitBatchRecommendationResponse(
            String region,
            String source,
            List<TimeSlotOutfitRecommendation> recommendations
    ) {
        this.region = region;
        this.source = source;
        this.recommendations = recommendations;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<TimeSlotOutfitRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(
            List<TimeSlotOutfitRecommendation> recommendations
    ) {
        this.recommendations = recommendations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeSlotOutfitRecommendation {

        private String timeSlot;
        private String timeSlotName;
        private LocalDateTime forecastAt;
        private LocalTime startTime;
        private LocalTime endTime;

        private AiOutfitRecommendationResponse.OutfitCards outfitCards;

        private List<AiOutfitRecommendationResponse.PreparationItem>
                preparationItems;

        public TimeSlotOutfitRecommendation() {
        }

        public TimeSlotOutfitRecommendation(
                String timeSlot,
                String timeSlotName,
                LocalDateTime forecastAt,
                LocalTime startTime,
                LocalTime endTime,
                AiOutfitRecommendationResponse.OutfitCards outfitCards,
                List<AiOutfitRecommendationResponse.PreparationItem>
                        preparationItems
        ) {
            this.timeSlot = timeSlot;
            this.timeSlotName = timeSlotName;
            this.forecastAt = forecastAt;
            this.startTime = startTime;
            this.endTime = endTime;
            this.outfitCards = outfitCards;
            this.preparationItems = preparationItems;
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

        public AiOutfitRecommendationResponse.OutfitCards
        getOutfitCards() {
            return outfitCards;
        }

        public void setOutfitCards(
                AiOutfitRecommendationResponse.OutfitCards outfitCards
        ) {
            this.outfitCards = outfitCards;
        }

        public List<AiOutfitRecommendationResponse.PreparationItem>
        getPreparationItems() {
            return preparationItems;
        }

        public void setPreparationItems(
                List<AiOutfitRecommendationResponse.PreparationItem>
                        preparationItems
        ) {
            this.preparationItems = preparationItems;
        }
    }
}
