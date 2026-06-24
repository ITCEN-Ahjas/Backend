package com.example.Chungbuk.domain.recommend.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRecommendationRequest {

    private String region;
    private List<String> interests;
    private String companionType;
    private String budget;
    private String activityIntensity;
    private String transportMode;
    private String travelDate;
    private String startTime;
    private String endTime;
    private String startLocation;
    private String endLocation;
    private List<Map<String, Object>> weatherTimeline;
    private List<Map<String, Object>> candidatePlaces;
}
