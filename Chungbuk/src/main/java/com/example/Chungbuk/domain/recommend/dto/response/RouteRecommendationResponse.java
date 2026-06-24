package com.example.Chungbuk.domain.recommend.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RouteRecommendationResponse {

    private String summary;
    private List<String> weatherNotes;
    private List<ItineraryItem> itinerary;
    private List<String> planB;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItineraryItem {

        private String time;
        private String placeName;
        private String description;
        private String weatherReason;
        private String moveTip;
    }
}
