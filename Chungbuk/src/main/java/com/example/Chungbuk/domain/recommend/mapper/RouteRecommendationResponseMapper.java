package com.example.Chungbuk.domain.recommend.mapper;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RouteRecommendationResponseMapper {

    public RouteRecommendationResponse toFrontendResponse(
            AiRouteRecommendationResponse aiResponse
    ) {
        return toFrontendResponse(aiResponse, Collections.emptyList());
    }

    public RouteRecommendationResponse toFrontendResponse(
            AiRouteRecommendationResponse aiResponse,
            List<AiRouteRecommendationRequest.CandidatePlace> candidatePlaces
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
                .itinerary(toItinerary(
                        aiResponse.getItinerary(),
                        candidatePlaces
                ))
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
            List<AiRouteRecommendationResponse.RoutePlace> itinerary,
            List<AiRouteRecommendationRequest.CandidatePlace> candidatePlaces
    ) {
        if (itinerary == null) {
            return Collections.emptyList();
        }

        CandidatePlaceIndex candidatePlaceIndex =
                new CandidatePlaceIndex(candidatePlaces);

        return itinerary.stream()
                .map(place -> toItineraryItem(place, candidatePlaceIndex))
                .toList();
    }

    private RouteRecommendationResponse.ItineraryItem toItineraryItem(
            AiRouteRecommendationResponse.RoutePlace place,
            CandidatePlaceIndex candidatePlaceIndex
    ) {
        AiRouteRecommendationRequest.CandidatePlace candidatePlace =
                candidatePlaceIndex.find(place);

        return RouteRecommendationResponse.ItineraryItem.builder()
                .day(place.getDay())
                .order(place.getOrder())
                .placeId(firstNonBlank(
                        getPlaceId(candidatePlace),
                        place.getPlaceId()
                ))
                .time(place.getStartTime())
                .startTime(place.getStartTime())
                .endTime(place.getEndTime())
                .placeName(place.getName())
                .category(place.getCategory())
                .indoor(place.isIndoor())
                .address(firstNonBlank(
                        getAddress(candidatePlace),
                        place.getAddress()
                ))
                .imageUrl(firstNonBlank(
                        getImageUrl(candidatePlace),
                        place.getImageUrl()
                ))
                .latitude(firstNonNull(
                        getLatitude(candidatePlace),
                        place.getLatitude()
                ))
                .longitude(firstNonNull(
                        getLongitude(candidatePlace),
                        place.getLongitude()
                ))
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

    private String getAddress(
            AiRouteRecommendationRequest.CandidatePlace candidatePlace
    ) {
        return candidatePlace == null ? null : candidatePlace.getAddress();
    }

    private String getPlaceId(
            AiRouteRecommendationRequest.CandidatePlace candidatePlace
    ) {
        return candidatePlace == null ? null : candidatePlace.getPlaceId();
    }

    private String getImageUrl(
            AiRouteRecommendationRequest.CandidatePlace candidatePlace
    ) {
        return candidatePlace == null ? null : candidatePlace.getImageUrl();
    }

    private Double getLatitude(
            AiRouteRecommendationRequest.CandidatePlace candidatePlace
    ) {
        return candidatePlace == null ? null : candidatePlace.getLatitude();
    }

    private Double getLongitude(
            AiRouteRecommendationRequest.CandidatePlace candidatePlace
    ) {
        return candidatePlace == null ? null : candidatePlace.getLongitude();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private Double firstNonNull(Double first, Double second) {
        if (first != null) {
            return first;
        }

        return second;
    }

    private String normalizeMatchKey(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", "")
                .toLowerCase();
    }

    private class CandidatePlaceIndex {

        private final Map<String, AiRouteRecommendationRequest.CandidatePlace>
                byPlaceId = new LinkedHashMap<>();
        private final Map<String, AiRouteRecommendationRequest.CandidatePlace>
                byName = new LinkedHashMap<>();

        CandidatePlaceIndex(
                List<AiRouteRecommendationRequest.CandidatePlace> places
        ) {
            if (places == null) {
                return;
            }

            for (AiRouteRecommendationRequest.CandidatePlace place : places) {
                if (place == null) {
                    continue;
                }

                putIfNotBlank(byPlaceId, place.getPlaceId(), place);
                putIfNotBlank(byName, normalizeMatchKey(place.getName()), place);
            }
        }

        AiRouteRecommendationRequest.CandidatePlace find(
                AiRouteRecommendationResponse.RoutePlace place
        ) {
            if (place == null) {
                return null;
            }

            AiRouteRecommendationRequest.CandidatePlace candidatePlace =
                    byPlaceId.get(place.getPlaceId());

            if (candidatePlace != null) {
                return candidatePlace;
            }

            return byName.get(normalizeMatchKey(place.getName()));
        }

        private void putIfNotBlank(
                Map<String, AiRouteRecommendationRequest.CandidatePlace> map,
                String key,
                AiRouteRecommendationRequest.CandidatePlace place
        ) {
            if (key == null || key.isBlank()) {
                return;
            }

            map.putIfAbsent(key, place);
        }
    }
}
