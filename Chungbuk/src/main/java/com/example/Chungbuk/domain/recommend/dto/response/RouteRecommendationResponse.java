package com.example.Chungbuk.domain.recommend.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RouteRecommendationResponse {

    private String region;
    private String source;
    private String summary;
    private RouteOverview routeOverview;
    private List<String> weatherNotes;
    private List<WeatherNote> weatherNoteDetails;
    private List<ItineraryItem> itinerary;
    private List<String> planB;
    private List<PlanBOption> planBOptions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RouteOverview {

        private String title;
        private String region;
        private int totalPlaces;
        private int totalStayMinutes;
        private String startLocation;
        private String endLocation;
        private List<String> styleTags;
        private String weatherSummary;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItineraryItem {

        private int day;
        private int order;
        private String placeId;
        private String time;
        private String startTime;
        private String endTime;
        private String placeName;
        private String category;
        private boolean indoor;
        private String address;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
        private String description;
        private String recommendationReason;
        private String weatherReason;
        private String moveTip;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlanBOption {

        private String triggerCondition;
        private String replaceFrom;
        private String replaceTo;
        private String reason;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class WeatherNote {

        private String timeRange;
        private String summary;
        private String cautionLevel;
    }
}
