package com.example.Chungbuk.domain.recommend.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRouteRecommendationResponse {

    private String region;
    private String source;
    private String summary;
    private RouteOverview routeOverview;
    private List<RoutePlace> itinerary;
    private List<PlanBOption> planB;
    private List<WeatherNote> weatherNotes;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutePlace {

        private int day;
        private int order;
        private String placeId;
        private String name;
        private String category;
        private String startTime;
        private String endTime;
        private boolean indoor;
        private String address;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
        private String recommendationReason;
        private String weatherReason;
        private String moveTip;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanBOption {

        private String triggerCondition;
        private String replaceFrom;
        private String replaceTo;
        private String reason;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherNote {

        private String timeRange;
        private String summary;
        private String cautionLevel;
    }
}
