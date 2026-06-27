package com.example.Chungbuk.domain.recommend.mapper;

import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RouteRecommendationResponseMapper {

    public RouteRecommendationResponse toFrontendResponse(
            AiRouteRecommendationResponse aiResponse
    ) {
        if (aiResponse == null) {
            return createEmptyResponse();
        }

        return RouteRecommendationResponse.builder()
                .region(aiResponse.getRegion())
                .source(aiResponse.getSource())
                .summary(aiResponse.getSummary())
                .routeOverview(toRouteOverview(aiResponse.getRouteOverview()))
                .weatherNotes(toWeatherNoteSummaries(aiResponse.getWeatherNotes()))
                .weatherNoteDetails(toWeatherNotes(aiResponse.getWeatherNotes()))
                .itinerary(toItinerary(aiResponse.getItinerary()))
                .planB(toPlanBSummaries(aiResponse.getPlanB()))
                .planBOptions(toPlanBOptions(aiResponse.getPlanB()))
                .build();
    }

    private RouteRecommendationResponse createEmptyResponse() {
        return RouteRecommendationResponse.builder()
                .source("fallback")
                .summary("추천 결과를 생성하지 못했습니다.")
                .weatherNotes(Collections.emptyList())
                .weatherNoteDetails(Collections.emptyList())
                .itinerary(Collections.emptyList())
                .planB(Collections.emptyList())
                .planBOptions(Collections.emptyList())
                .build();
    }

    private RouteRecommendationResponse.RouteOverview toRouteOverview(
            AiRouteRecommendationResponse.RouteOverview overview
    ) {
        if (overview == null) {
            return null;
        }

        return RouteRecommendationResponse.RouteOverview.builder()
                .title(overview.getTitle())
                .region(overview.getRegion())
                .totalPlaces(overview.getTotalPlaces())
                .totalStayMinutes(overview.getTotalStayMinutes())
                .startLocation(overview.getStartLocation())
                .endLocation(overview.getEndLocation())
                .styleTags(nullToEmpty(overview.getStyleTags()))
                .weatherSummary(overview.getWeatherSummary())
                .build();
    }

    private List<RouteRecommendationResponse.ItineraryItem> toItinerary(
            List<AiRouteRecommendationResponse.RoutePlace> itinerary
    ) {
        if (itinerary == null) {
            return Collections.emptyList();
        }

        return itinerary.stream()
                .map(this::toItineraryItem)
                .toList();
    }

    private RouteRecommendationResponse.ItineraryItem toItineraryItem(
            AiRouteRecommendationResponse.RoutePlace place
    ) {
        return RouteRecommendationResponse.ItineraryItem.builder()
                .day(place.getDay())
                .order(place.getOrder())
                .placeId(place.getPlaceId())
                .time(place.getStartTime())
                .startTime(place.getStartTime())
                .endTime(place.getEndTime())
                .placeName(place.getName())
                .category(place.getCategory())
                .indoor(place.isIndoor())
                .address(place.getAddress())
                .imageUrl(place.getImageUrl())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .description(place.getRecommendationReason())
                .recommendationReason(place.getRecommendationReason())
                .weatherReason(place.getWeatherReason())
                .moveTip(place.getMoveTip())
                .build();
    }

    private List<String> toPlanBSummaries(
            List<AiRouteRecommendationResponse.PlanBOption> planB
    ) {
        if (planB == null) {
            return Collections.emptyList();
        }

        return planB.stream()
                .map(option -> option.getTriggerCondition()
                        + ": "
                        + option.getReplaceFrom()
                        + " -> "
                        + option.getReplaceTo()
                        + " - "
                        + option.getReason())
                .toList();
    }

    private List<RouteRecommendationResponse.PlanBOption> toPlanBOptions(
            List<AiRouteRecommendationResponse.PlanBOption> planB
    ) {
        if (planB == null) {
            return Collections.emptyList();
        }

        return planB.stream()
                .map(option -> RouteRecommendationResponse.PlanBOption.builder()
                        .triggerCondition(option.getTriggerCondition())
                        .replaceFrom(option.getReplaceFrom())
                        .replaceTo(option.getReplaceTo())
                        .reason(option.getReason())
                        .build())
                .toList();
    }

    private List<String> toWeatherNoteSummaries(
            List<AiRouteRecommendationResponse.WeatherNote> weatherNotes
    ) {
        if (weatherNotes == null) {
            return Collections.emptyList();
        }

        return weatherNotes.stream()
                .map(note -> note.getTimeRange()
                        + ": "
                        + note.getSummary()
                        + " ("
                        + note.getCautionLevel()
                        + ")")
                .toList();
    }

    private List<RouteRecommendationResponse.WeatherNote> toWeatherNotes(
            List<AiRouteRecommendationResponse.WeatherNote> weatherNotes
    ) {
        if (weatherNotes == null) {
            return Collections.emptyList();
        }

        return weatherNotes.stream()
                .map(note -> RouteRecommendationResponse.WeatherNote.builder()
                        .timeRange(note.getTimeRange())
                        .summary(note.getSummary())
                        .cautionLevel(note.getCautionLevel())
                        .build())
                .toList();
    }

    private List<String> nullToEmpty(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values;
    }
}
