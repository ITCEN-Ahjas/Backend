package com.example.Chungbuk.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteRecommendationFallbackServiceTest {

    private final RouteRecommendationFallbackService fallbackService =
            new RouteRecommendationFallbackService();

    @Test
    void createsFallbackResponseFromCandidatePlaces() {
        RouteRecommendationResponse response =
                fallbackService.createFallbackResponse(createAiRequest());

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getRouteOverview().getTotalPlaces())
                .isEqualTo(1);
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getPlaceId())
                .isEqualTo("place-1");
        assertThat(response.getItinerary().get(0).getLatitude())
                .isEqualTo(36.65);
        assertThat(response.getWeatherNoteDetails()).hasSize(1);
        assertThat(response.getPlanB()).hasSize(1);
    }

    @Test
    void createsFallbackResponseWithDefaultsWhenOptionalFieldsAreMissing() {
        AiRouteRecommendationRequest aiRequest =
                AiRouteRecommendationRequest.builder()
                        .region("Cheongju")
                        .candidatePlaces(List.of(
                                AiRouteRecommendationRequest.CandidatePlace.builder()
                                        .placeId("place-1")
                                        .name("Sangdang Sanseong")
                                        .category("landmark")
                                        .latitude(36.65)
                                        .longitude(127.49)
                                        .averageStayMinutes(90)
                                        .build()
                        ))
                        .build();

        RouteRecommendationResponse response =
                fallbackService.createFallbackResponse(aiRequest);

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getStartTime())
                .isEqualTo("09:00");
        assertThat(response.getRouteOverview().getStartLocation())
                .isEqualTo("Start location");
        assertThat(response.getPlanB()).isNotEmpty();
    }

    @Test
    void sortsFallbackItineraryByNearestCoordinates() {
        AiRouteRecommendationRequest aiRequest = createAiRequestWithPlaces(
                List.of(
                        createCandidatePlace(
                                "place-a",
                                "Start Area",
                                36.60,
                                127.40
                        ),
                        createCandidatePlace(
                                "place-c",
                                "Far Area",
                                37.10,
                                128.10
                        ),
                        createCandidatePlace(
                                "place-b",
                                "Near Area",
                                36.61,
                                127.41
                        )
                )
        );

        RouteRecommendationResponse response =
                fallbackService.createFallbackResponse(aiRequest);

        assertThat(response.getItinerary())
                .extracting(RouteRecommendationResponse.ItineraryItem::getPlaceId)
                .containsExactly("place-a", "place-b", "place-c");
        assertThat(response.getItinerary())
                .extracting(RouteRecommendationResponse.ItineraryItem::getOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void placesWithoutCoordinatesArePlacedAfterMappablePlaces() {
        AiRouteRecommendationRequest aiRequest = createAiRequestWithPlaces(
                List.of(
                        createCandidatePlace(
                                "place-a",
                                "Start Area",
                                36.60,
                                127.40
                        ),
                        createCandidatePlace(
                                "place-without-coordinate",
                                "Unknown Area",
                                null,
                                null
                        ),
                        createCandidatePlace(
                                "place-b",
                                "Near Area",
                                36.61,
                                127.41
                        )
                )
        );

        RouteRecommendationResponse response =
                fallbackService.createFallbackResponse(aiRequest);

        assertThat(response.getItinerary())
                .extracting(RouteRecommendationResponse.ItineraryItem::getPlaceId)
                .containsExactly(
                        "place-a",
                        "place-b",
                        "place-without-coordinate"
                );
    }

    private AiRouteRecommendationRequest createAiRequest() {
        return AiRouteRecommendationRequest.builder()
                .region("Cheongju")
                .preference(AiRouteRecommendationRequest.Preference.builder()
                        .interests(List.of("nature"))
                        .companionType("friends")
                        .budgetLevel("medium")
                        .activityPace("balanced")
                        .transportMode("car")
                        .build())
                .constraint(AiRouteRecommendationRequest.Constraint.builder()
                        .travelDate("2026-06-24")
                        .startTime("09:00")
                        .endTime("18:00")
                        .startLocation("Cheongju Station")
                        .endLocation("Cheongju Station")
                        .build())
                .weatherTimeline(List.of(
                        AiRouteRecommendationRequest.HourlyWeather.builder()
                                .time("09:00")
                                .condition("clear")
                                .precipitationProbability(10)
                                .temperature(24)
                                .feelsLikeTemperature(24)
                                .fineDustLevel("normal")
                                .build()
                ))
                .candidatePlaces(List.of(
                        AiRouteRecommendationRequest.CandidatePlace.builder()
                                .placeId("place-1")
                                .name("Sangdang Sanseong")
                                .category("landmark")
                                .interests(List.of("nature"))
                                .indoor(false)
                                .address("Cheongju, Chungbuk")
                                .latitude(36.65)
                                .longitude(127.49)
                                .averageStayMinutes(90)
                                .openTime("09:00")
                                .closeTime("20:00")
                                .build()
                ))
                .build();
    }

    private AiRouteRecommendationRequest createAiRequestWithPlaces(
            List<AiRouteRecommendationRequest.CandidatePlace> places
    ) {
        return AiRouteRecommendationRequest.builder()
                .region("Cheongju")
                .preference(AiRouteRecommendationRequest.Preference.builder()
                        .interests(List.of("nature"))
                        .companionType("friends")
                        .budgetLevel("medium")
                        .activityPace("balanced")
                        .transportMode("car")
                        .build())
                .constraint(AiRouteRecommendationRequest.Constraint.builder()
                        .travelDate("2026-06-24")
                        .startTime("09:00")
                        .endTime("18:00")
                        .startLocation("Cheongju Station")
                        .endLocation("Cheongju Station")
                        .build())
                .weatherTimeline(List.of())
                .candidatePlaces(places)
                .build();
    }

    private AiRouteRecommendationRequest.CandidatePlace createCandidatePlace(
            String placeId,
            String name,
            Double latitude,
            Double longitude
    ) {
        return AiRouteRecommendationRequest.CandidatePlace.builder()
                .placeId(placeId)
                .name(name)
                .category("landmark")
                .interests(List.of("nature"))
                .indoor(false)
                .address("Cheongju, Chungbuk")
                .latitude(latitude)
                .longitude(longitude)
                .averageStayMinutes(90)
                .openTime("09:00")
                .closeTime("20:00")
                .build();
    }
}
