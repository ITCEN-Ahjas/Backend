package com.example.Chungbuk.domain.recommend.service;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RouteRecommendationFallbackService {

    private static final double EARTH_RADIUS_KILOMETERS = 6371.0;

    public RouteRecommendationResponse createFallbackResponse(
            AiRouteRecommendationRequest aiRequest
    ) {
        List<AiRouteRecommendationRequest.CandidatePlace> places =
                getCandidatePlaces(aiRequest);
        AiRouteRecommendationRequest.Preference preference =
                getPreference(aiRequest);
        AiRouteRecommendationRequest.Constraint constraint =
                getConstraint(aiRequest);
        String region = valueOrDefault(
                aiRequest == null ? null : aiRequest.getRegion(),
                "Chungbuk"
        );
        List<AiRouteRecommendationRequest.CandidatePlace> sortedPlaces =
                sortByNearestCoordinate(places);
        List<AiRouteRecommendationRequest.CandidatePlace> selectedPlaces =
                sortedPlaces.stream()
                        .limit(3)
                        .toList();

        List<RouteRecommendationResponse.ItineraryItem> itinerary =
                selectedPlaces.stream()
                        .map(place -> toItineraryItem(
                                preference,
                                constraint,
                                place,
                                selectedPlaces.indexOf(place) + 1
                        ))
                        .toList();

        int totalStayMinutes = selectedPlaces.stream()
                .mapToInt(AiRouteRecommendationRequest.CandidatePlace::getAverageStayMinutes)
                .sum();
        String totalDistance = formatDistance(calculateRouteDistance(
                itinerary
        ));
        String totalDuration = formatDuration(totalStayMinutes);

        return RouteRecommendationResponse.builder()
                .region(region)
                .source("fallback")
                .summary("AI server is unavailable, so a basic route was created from candidate places.")
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .routeOverview(RouteRecommendationResponse.RouteOverview.builder()
                        .title(region + " fallback travel route")
                        .region(region)
                        .totalPlaces(itinerary.size())
                        .totalStayMinutes(totalStayMinutes)
                        .totalDistance(totalDistance)
                        .totalDuration(totalDuration)
                        .startLocation(constraint.getStartLocation())
                        .endLocation(constraint.getEndLocation())
                        .styleTags(List.of(
                                preference.getActivityPace(),
                                preference.getTransportMode()
                        ))
                        .weatherSummary("Fallback route uses the available weather and place data.")
                        .build())
                .weatherNotes(List.of(
                        "AI server did not respond, so weather notes were simplified."
                ))
                .weatherNoteDetails(List.of(
                        RouteRecommendationResponse.WeatherNote.builder()
                                .timeRange(constraint.getStartTime()
                                        + "-"
                                        + constraint.getEndTime())
                                .summary("Fallback route uses basic weather assumptions.")
                                .cautionLevel("low")
                                .build()
                ))
                .itinerary(itinerary)
                .planB(List.of(
                        "If weather worsens, prioritize indoor places such as restaurants, cafes, museums, or shopping spots."
                ))
                .planBOptions(Collections.emptyList())
                .build();
    }

    private RouteRecommendationResponse.ItineraryItem toItineraryItem(
            AiRouteRecommendationRequest.Preference preference,
            AiRouteRecommendationRequest.Constraint constraint,
            AiRouteRecommendationRequest.CandidatePlace place,
            int order
    ) {
        String startTime = constraint.getStartTime();

        return RouteRecommendationResponse.ItineraryItem.builder()
                .day(1)
                .order(order)
                .placeId(place.getPlaceId())
                .time(startTime)
                .startTime(startTime)
                .endTime(constraint.getEndTime())
                .placeName(place.getName())
                .category(place.getCategory())
                .indoor(place.isIndoor())
                .address(place.getAddress())
                .imageUrl(place.getImageUrl())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .description("Selected from available candidate places.")
                .recommendationReason("Selected from available candidate places.")
                .weatherReason("Fallback route keeps the schedule simple when AI recommendation is unavailable.")
                .moveTip(createMoveTip(preference.getTransportMode()))
                .build();
    }

    private List<AiRouteRecommendationRequest.CandidatePlace>
    sortByNearestCoordinate(
            List<AiRouteRecommendationRequest.CandidatePlace> places
    ) {
        if (places.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiRouteRecommendationRequest.CandidatePlace> remaining =
                new ArrayList<>(places);
        List<AiRouteRecommendationRequest.CandidatePlace> sorted =
                new ArrayList<>();

        AiRouteRecommendationRequest.CandidatePlace current =
                removeFirstPlaceWithCoordinate(remaining);

        if (current == null) {
            return remaining;
        }

        sorted.add(current);

        while (!remaining.isEmpty()) {
            AiRouteRecommendationRequest.CandidatePlace next =
                    removeNearestPlace(remaining, current);
            sorted.add(next);
            current = next;
        }

        return sorted;
    }

    private AiRouteRecommendationRequest.CandidatePlace
    removeFirstPlaceWithCoordinate(
            List<AiRouteRecommendationRequest.CandidatePlace> places
    ) {
        for (int index = 0; index < places.size(); index++) {
            AiRouteRecommendationRequest.CandidatePlace place =
                    places.get(index);

            if (hasCoordinate(place)) {
                return places.remove(index);
            }
        }

        return null;
    }

    private AiRouteRecommendationRequest.CandidatePlace removeNearestPlace(
            List<AiRouteRecommendationRequest.CandidatePlace> places,
            AiRouteRecommendationRequest.CandidatePlace current
    ) {
        int nearestIndex = 0;
        double nearestDistance = Double.MAX_VALUE;

        for (int index = 0; index < places.size(); index++) {
            AiRouteRecommendationRequest.CandidatePlace place =
                    places.get(index);
            double distance = calculateDistance(current, place);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }

        return places.remove(nearestIndex);
    }

    private double calculateDistance(
            AiRouteRecommendationRequest.CandidatePlace current,
            AiRouteRecommendationRequest.CandidatePlace next
    ) {
        if (!hasCoordinate(current) || !hasCoordinate(next)) {
            return Double.MAX_VALUE;
        }

        double latitudeDiff = current.getLatitude() - next.getLatitude();
        double longitudeDiff = current.getLongitude() - next.getLongitude();

        return latitudeDiff * latitudeDiff + longitudeDiff * longitudeDiff;
    }

    private boolean hasCoordinate(
            AiRouteRecommendationRequest.CandidatePlace place
    ) {
        return place != null
                && place.getLatitude() != null
                && place.getLongitude() != null;
    }

    private double calculateRouteDistance(
            List<RouteRecommendationResponse.ItineraryItem> itinerary
    ) {
        if (itinerary == null || itinerary.size() < 2) {
            return 0.0;
        }

        double distance = 0.0;
        RouteRecommendationResponse.ItineraryItem previous = null;

        for (RouteRecommendationResponse.ItineraryItem current : itinerary) {
            if (!hasCoordinate(current)) {
                continue;
            }

            if (previous != null) {
                distance += calculateDistance(previous, current);
            }

            previous = current;
        }

        return distance;
    }

    private boolean hasCoordinate(
            RouteRecommendationResponse.ItineraryItem item
    ) {
        return item != null
                && item.getLatitude() != null
                && item.getLongitude() != null;
    }

    private double calculateDistance(
            RouteRecommendationResponse.ItineraryItem first,
            RouteRecommendationResponse.ItineraryItem second
    ) {
        double latitudeDistance = Math.toRadians(
                second.getLatitude() - first.getLatitude()
        );
        double longitudeDistance = Math.toRadians(
                second.getLongitude() - first.getLongitude()
        );
        double firstLatitude = Math.toRadians(first.getLatitude());
        double secondLatitude = Math.toRadians(second.getLatitude());
        double haversine = Math.sin(latitudeDistance / 2)
                * Math.sin(latitudeDistance / 2)
                + Math.cos(firstLatitude)
                * Math.cos(secondLatitude)
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);

        return EARTH_RADIUS_KILOMETERS
                * 2
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private String formatDistance(double distance) {
        if (distance <= 0.0) {
            return "0.0km";
        }

        return String.format(Locale.US, "%.1fkm", distance);
    }

    private String formatDuration(int totalStayMinutes) {
        if (totalStayMinutes <= 0) {
            return "0min";
        }

        int hours = totalStayMinutes / 60;
        int minutes = totalStayMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "min";
        }

        if (hours > 0) {
            return hours + "h";
        }

        return minutes + "min";
    }

    private String createMoveTip(String transportMode) {
        return switch (transportMode) {
            case "walk" -> "Keep the route compact for walking.";
            case "public_transport" -> "Check public transport intervals before moving.";
            case "taxi" -> "Use taxi for long gaps between places.";
            default -> "Check parking and congestion before moving by car.";
        };
    }

    private List<AiRouteRecommendationRequest.CandidatePlace> getCandidatePlaces(
            AiRouteRecommendationRequest aiRequest
    ) {
        if (aiRequest == null || aiRequest.getCandidatePlaces() == null) {
            return Collections.emptyList();
        }

        return aiRequest.getCandidatePlaces();
    }

    private AiRouteRecommendationRequest.Preference getPreference(
            AiRouteRecommendationRequest aiRequest
    ) {
        if (aiRequest != null && aiRequest.getPreference() != null) {
            AiRouteRecommendationRequest.Preference preference =
                    aiRequest.getPreference();
            return AiRouteRecommendationRequest.Preference.builder()
                    .interests(nullToEmpty(preference.getInterests()))
                    .companionType(valueOrDefault(
                            preference.getCompanionType(),
                            "unknown"
                    ))
                    .budgetLevel(valueOrDefault(
                            preference.getBudgetLevel(),
                            "medium"
                    ))
                    .activityPace(valueOrDefault(
                            preference.getActivityPace(),
                            "balanced"
                    ))
                    .transportMode(valueOrDefault(
                            preference.getTransportMode(),
                            "car"
                    ))
                    .build();
        }

        return AiRouteRecommendationRequest.Preference.builder()
                .interests(Collections.emptyList())
                .companionType("unknown")
                .budgetLevel("medium")
                .activityPace("balanced")
                .transportMode("car")
                .build();
    }

    private AiRouteRecommendationRequest.Constraint getConstraint(
            AiRouteRecommendationRequest aiRequest
    ) {
        if (aiRequest != null && aiRequest.getConstraint() != null) {
            AiRouteRecommendationRequest.Constraint constraint =
                    aiRequest.getConstraint();
            return AiRouteRecommendationRequest.Constraint.builder()
                    .travelDate(constraint.getTravelDate())
                    .startTime(valueOrDefault(
                            constraint.getStartTime(),
                            "09:00"
                    ))
                    .endTime(valueOrDefault(
                            constraint.getEndTime(),
                            "18:00"
                    ))
                    .startLocation(valueOrDefault(
                            constraint.getStartLocation(),
                            "Start location"
                    ))
                    .endLocation(valueOrDefault(
                            constraint.getEndLocation(),
                            "End location"
                    ))
                    .build();
        }

        return AiRouteRecommendationRequest.Constraint.builder()
                .startTime("09:00")
                .endTime("18:00")
                .startLocation("Start location")
                .endLocation("End location")
                .build();
    }

    private List<String> nullToEmpty(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values;
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}
