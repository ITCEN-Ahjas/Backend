package com.example.Chungbuk.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import com.example.Chungbuk.domain.place.service.PlaceSearchService;
import com.example.Chungbuk.domain.recommend.client.AiRouteRecommendationClient;
import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationMapper;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationResponseMapper;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import com.example.Chungbuk.global.config.AiRouteRecommendationProperties;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class RouteRecommendationAiIntegrationTest {

    private final WeatherService weatherService = mock(WeatherService.class);
    private final PlaceSearchService placeSearchService =
            mock(PlaceSearchService.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final RouteRecommendationService service =
            createRouteRecommendationService();

    @Test
    void recommendRoutesConvertsFrontendRequestToAiRequestAndMapsAiResponse() {
        RouteRecommendationRequest request = createFrontendRequest();
        AiRouteRecommendationResponse aiResponse = createAiResponse();

        when(weatherService.getRegionTimeSlotWeather(any()))
                .thenReturn(createWeatherResponse());
        when(placeSearchService.search(
                eq("Cheongju"),
                eq(PlaceCategory.RESTAURANT),
                eq(5),
                eq(null)
        )).thenReturn(createPlaceResponse());
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiRouteRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(aiResponse));

        RouteRecommendationResponse response = service.recommend(request);

        assertThat(response.getSource()).isEqualTo("ai");
        assertThat(response.getSummary()).isEqualTo("AI generated route.");
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getPlaceName())
                .isEqualTo("Local Restaurant");
        assertThat(response.getItinerary().get(0).getLatitude())
                .isEqualTo(36.65);
        assertThat(response.getWeatherNotes()).hasSize(1);
        assertThat(response.getPlanB()).hasSize(1);

        AiRouteRecommendationRequest aiRequest = captureAiRequest();

        assertThat(aiRequest.getRegion()).isEqualTo("Cheongju");
        assertThat(aiRequest.getPreference().getActivityPace())
                .isEqualTo("balanced");
        assertThat(aiRequest.getPreference().getTransportMode())
                .isEqualTo("public_transport");
        assertThat(aiRequest.getWeatherTimeline()).hasSize(1);
        assertThat(aiRequest.getWeatherTimeline().get(0).getCondition())
                .isEqualTo("rain");
        assertThat(aiRequest.getCandidatePlaces()).hasSize(1);
        assertThat(aiRequest.getCandidatePlaces().get(0).getCategory())
                .isEqualTo("restaurant");
        assertThat(aiRequest.getCandidatePlaces().get(0).getLatitude())
                .isEqualTo(36.65);
    }

    @Test
    void recommendRoutesReturnsFallbackResponseWhenAiServerFails() {
        RouteRecommendationRequest request = createFrontendRequest();

        when(weatherService.getRegionTimeSlotWeather(any()))
                .thenReturn(createWeatherResponse());
        when(placeSearchService.search(
                eq("Cheongju"),
                eq(PlaceCategory.RESTAURANT),
                eq(5),
                eq(null)
        )).thenReturn(createPlaceResponse());
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiRouteRecommendationResponse.class)
        )).thenThrow(new RestClientException("connection failed"));

        RouteRecommendationResponse response = service.recommend(request);

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getItinerary()).hasSize(1);
        assertThat(response.getItinerary().get(0).getPlaceName())
                .isEqualTo("Local Restaurant");
        assertThat(response.getItinerary().get(0).getLatitude())
                .isEqualTo(36.65);
        assertThat(response.getSummary()).contains("AI server");
        assertThat(response.getWeatherNotes()).isNotEmpty();
        assertThat(response.getPlanB()).isNotEmpty();
    }

    private RouteRecommendationService createRouteRecommendationService() {
        RouteRecommendationMapper requestMapper =
                new RouteRecommendationMapper();
        RouteRecommendationDataAssembler dataAssembler =
                new RouteRecommendationDataAssembler(
                        weatherService,
                        placeSearchService,
                        requestMapper
                );
        AiRouteRecommendationClient aiClient =
                new AiRouteRecommendationClient(
                        restTemplate,
                        createAiRouteRecommendationProperties()
                );

        return new RouteRecommendationService(
                dataAssembler,
                aiClient,
                new RouteRecommendationResponseMapper(),
                new RouteRecommendationFallbackService()
        );
    }

    private AiRouteRecommendationProperties
    createAiRouteRecommendationProperties() {
        AiRouteRecommendationProperties properties =
                new AiRouteRecommendationProperties();
        properties.setBaseUrl("http://localhost:8000");
        properties.setRoutesPath("/api/v1/recommend/routes");

        return properties;
    }

    private RouteRecommendationRequest createFrontendRequest() {
        RouteRecommendationRequest request =
                new RouteRecommendationRequest();

        request.setRegion("Cheongju");
        request.setInterests(List.of("food"));
        request.setCompanionType("friends");
        request.setBudget("medium");
        request.setActivityIntensity("medium");
        request.setTransportMode("publicTransit");
        request.setTravelDate("2026-06-24");
        request.setStartTime("09:00");
        request.setEndTime("18:00");
        request.setStartLocation("Cheongju Station");
        request.setEndLocation("Cheongju Station");
        request.setWeatherTimeline(List.of());
        request.setCandidatePlaces(List.of());

        return request;
    }

    private RegionTimeSlotWeatherResponse createWeatherResponse() {
        return new RegionTimeSlotWeatherResponse(
                "Cheongju",
                LocalDateTime.of(2026, 6, 24, 8, 0),
                LocalDate.of(2026, 6, 24),
                List.of(
                        new TimeSlotWeatherResponse(
                                WeatherTimeSlot.MORNING,
                                LocalDateTime.of(2026, 6, 24, 9, 0),
                                new CurrentWeatherResponse(
                                        "Cheongju",
                                        24.0,
                                        70,
                                        2.0,
                                        "normal",
                                        "0mm",
                                        "rain",
                                        80,
                                        "cloudy",
                                        "rain"
                                ),
                                new FeelsLikeWeatherResponse(
                                        25.0,
                                        1.0,
                                        "Feels slightly warmer.",
                                        "Warm",
                                        "Humidity raises perceived temperature.",
                                        List.of("humidity")
                                )
                        )
                )
        );
    }

    private PlaceSearchResponse createPlaceResponse() {
        return PlaceSearchResponse.builder()
                .items(List.of(
                        PlaceSummaryResponse.builder()
                                .placeId("restaurant-1")
                                .name("Local Restaurant")
                                .address("Cheongju, Chungbuk")
                                .latitude(36.65)
                                .longitude(127.49)
                                .category("restaurant")
                                .primaryType("restaurant")
                                .primaryTypeName("Restaurant")
                                .types(List.of("restaurant"))
                                .rating(4.5)
                                .userRatingCount(100)
                                .photoName("places/restaurant-1/photos/photo-1")
                                .googleMapsUri("https://maps.example/restaurant-1")
                                .build()
                ))
                .size(1)
                .nextPageToken(null)
                .build();
    }

    private AiRouteRecommendationResponse createAiResponse() {
        AiRouteRecommendationResponse response =
                new AiRouteRecommendationResponse();

        response.setRegion("Cheongju");
        response.setSource("ai");
        response.setSummary("AI generated route.");

        AiRouteRecommendationResponse.RouteOverview overview =
                new AiRouteRecommendationResponse.RouteOverview();
        overview.setTitle("Cheongju AI route");
        overview.setRegion("Cheongju");
        overview.setTotalPlaces(1);
        overview.setTotalStayMinutes(80);
        overview.setStartLocation("Cheongju Station");
        overview.setEndLocation("Cheongju Station");
        overview.setStyleTags(List.of("balanced", "public_transport"));
        overview.setWeatherSummary("Rain is likely near lunch time.");
        response.setRouteOverview(overview);

        AiRouteRecommendationResponse.RoutePlace place =
                new AiRouteRecommendationResponse.RoutePlace();
        place.setDay(1);
        place.setOrder(1);
        place.setPlaceId("restaurant-1");
        place.setName("Local Restaurant");
        place.setCategory("restaurant");
        place.setStartTime("12:00");
        place.setEndTime("13:20");
        place.setIndoor(true);
        place.setAddress("Cheongju, Chungbuk");
        place.setLatitude(36.65);
        place.setLongitude(127.49);
        place.setRecommendationReason("Matches the user's food preference.");
        place.setWeatherReason("Indoor place is useful during rain.");
        place.setMoveTip("Check public transport intervals.");
        response.setItinerary(List.of(place));

        AiRouteRecommendationResponse.PlanBOption planB =
                new AiRouteRecommendationResponse.PlanBOption();
        planB.setTriggerCondition("heavy rain");
        planB.setReplaceFrom("Outdoor stop");
        planB.setReplaceTo("Local Restaurant");
        planB.setReason("Indoor place reduces weather risk.");
        response.setPlanB(List.of(planB));

        AiRouteRecommendationResponse.WeatherNote weatherNote =
                new AiRouteRecommendationResponse.WeatherNote();
        weatherNote.setTimeRange("12:00-15:00");
        weatherNote.setSummary("Rain is likely.");
        weatherNote.setCautionLevel("medium");
        response.setWeatherNotes(List.of(weatherNote));

        return response;
    }

    @SuppressWarnings("unchecked")
    private AiRouteRecommendationRequest captureAiRequest() {
        ArgumentCaptor<HttpEntity<AiRouteRecommendationRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(AiRouteRecommendationResponse.class)
        );

        return captor.getValue().getBody();
    }
}
