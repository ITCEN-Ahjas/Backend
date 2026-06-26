package com.example.Chungbuk.domain.recommend.dto.ai.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiRouteRecommendationRequest {

    private String region;
    private Preference preference;
    private Constraint constraint;
    private List<HourlyWeather> weatherTimeline;
    private List<CandidatePlace> candidatePlaces;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Preference {

        private List<String> interests;
        private String companionType;
        private String budgetLevel;
        private String activityPace;
        private String transportMode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Constraint {

        private String travelDate;
        private String startTime;
        private String endTime;
        private String startLocation;
        private String endLocation;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HourlyWeather {

        private String time;
        private String condition;
        private int precipitationProbability;
        private double temperature;
        private double feelsLikeTemperature;
        private String fineDustLevel;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CandidatePlace {

        private String placeId;
        private String name;
        private String category;
        private List<String> interests;
        private boolean indoor;
        private String address;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
        private int averageStayMinutes;
        private String openTime;
        private String closeTime;
    }
}
