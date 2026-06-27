package com.example.Chungbuk.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.recommend.client.AiRouteRecommendationClient;
import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationResponseMapper;
import com.example.Chungbuk.global.exception.AiRouteRecommendationApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteRecommendationServiceTest {

    private final RouteRecommendationDataAssembler dataAssembler =
            mock(RouteRecommendationDataAssembler.class);
    private final AiRouteRecommendationClient aiClient =
            mock(AiRouteRecommendationClient.class);
    private final RouteRecommendationResponseMapper responseMapper =
            mock(RouteRecommendationResponseMapper.class);
    private final RouteRecommendationFallbackService fallbackService =
            new RouteRecommendationFallbackService();
    private final RouteRecommendationService service =
            new RouteRecommendationService(
                    dataAssembler,
                    aiClient,
                    responseMapper,
                    fallbackService
            );

    @Test
    void recommendReturnsMappedAiResponseWhenAiServerResponds() {
        AiRouteRecommendationRequest aiRequest = createAiRequest();
        AiRouteRecommendationResponse aiResponse = createAiResponse();
        RouteRecommendationResponse expectedResponse =
                RouteRecommendationResponse.builder()
                        .source("ai")
                        .summary("AI route")
                        .weatherNotes(List.of("Weather note"))
                        .itinerary(List.of(
                                RouteRecommendationResponse.ItineraryItem.builder()
                                        .placeId("place-1")
                                        .placeName("Sangdang Sanseong")
                                        .build()
                        ))
                        .planB(List.of("Indoor alternative"))
                        .build();

        when(dataAssembler.assemble(any())).thenReturn(aiRequest);
        when(aiClient.recommend(aiRequest)).thenReturn(aiResponse);
        when(responseMapper.toFrontendResponse(
                eq(aiResponse),
                eq(aiRequest.getCandidatePlaces())
        ))
                .thenReturn(expectedResponse);

        RouteRecommendationResponse actualResponse =
                service.recommend(new RouteRecommendationRequest());

        assertThat(actualResponse.getSource()).isEqualTo("ai");
        assertThat(actualResponse.getSummary()).isEqualTo("AI route");
    }

    @Test
    void recommendReturnsFallbackResponseWhenAiServerFails() {
        AiRouteRecommendationRequest aiRequest = createAiRequest();

        when(dataAssembler.assemble(any())).thenReturn(aiRequest);
        when(aiClient.recommend(aiRequest))
                .thenThrow(new AiRouteRecommendationApiException("failed"));

        RouteRecommendationResponse response =
                service.recommend(new RouteRecommendationRequest());

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getPlaceId())
                .isEqualTo("place-1");
    }

    @Test
    void recommendReturnsFallbackResponseWhenAiResponseIsInvalid() {
        AiRouteRecommendationRequest aiRequest = createAiRequest();
        AiRouteRecommendationResponse invalidAiResponse =
                new AiRouteRecommendationResponse();

        when(dataAssembler.assemble(any())).thenReturn(aiRequest);
        when(aiClient.recommend(aiRequest)).thenReturn(invalidAiResponse);

        RouteRecommendationResponse response =
                service.recommend(new RouteRecommendationRequest());

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getPlaceId())
                .isEqualTo("place-1");
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
                .weatherTimeline(List.of())
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

    private AiRouteRecommendationResponse createAiResponse() {
        AiRouteRecommendationResponse response =
                new AiRouteRecommendationResponse();

        response.setRegion("Cheongju");
        response.setSource("ai");
        response.setSummary("AI route");

        AiRouteRecommendationResponse.RouteOverview overview =
                new AiRouteRecommendationResponse.RouteOverview();
        overview.setTitle("Cheongju AI route");
        overview.setRegion("Cheongju");
        overview.setTotalPlaces(1);
        overview.setTotalStayMinutes(90);
        overview.setStartLocation("Cheongju Station");
        overview.setEndLocation("Cheongju Station");
        overview.setStyleTags(List.of("balanced", "car"));
        overview.setWeatherSummary("Clear weather is expected.");
        response.setRouteOverview(overview);

        AiRouteRecommendationResponse.RoutePlace place =
                new AiRouteRecommendationResponse.RoutePlace();
        place.setDay(1);
        place.setOrder(1);
        place.setPlaceId("place-1");
        place.setName("Sangdang Sanseong");
        place.setCategory("landmark");
        place.setStartTime("09:00");
        place.setEndTime("10:30");
        place.setIndoor(false);
        place.setAddress("Cheongju, Chungbuk");
        place.setLatitude(36.65);
        place.setLongitude(127.49);
        place.setRecommendationReason("Matches the user's nature preference.");
        place.setWeatherReason("Good weather for outdoor walking.");
        place.setMoveTip("Move by car.");
        response.setItinerary(List.of(place));

        return response;
    }
}
