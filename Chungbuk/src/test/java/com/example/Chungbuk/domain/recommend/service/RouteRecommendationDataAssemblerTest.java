package com.example.Chungbuk.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import com.example.Chungbuk.domain.place.service.PlaceSearchService;
import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationMapper;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteRecommendationDataAssemblerTest {

    private final WeatherService weatherService =
            mock(WeatherService.class);
    private final PlaceSearchService placeSearchService =
            mock(PlaceSearchService.class);
    private final RouteRecommendationDataAssembler assembler =
            new RouteRecommendationDataAssembler(
                    weatherService,
                    placeSearchService,
                    new RouteRecommendationMapper()
            );

    @Test
    void fillsWeatherTimelineAndCandidatePlacesWhenRequestListsAreEmpty() {
        RouteRecommendationRequest request = createRequest();

        when(weatherService.getRegionTimeSlotWeather(any()))
                .thenReturn(createWeatherResponse());
        when(placeSearchService.search(
                eq("Cheongju"),
                eq(PlaceCategory.TOURIST_ATTRACTION),
                eq(5),
                eq(null)
        )).thenReturn(createPlaceResponse());

        AiRouteRecommendationRequest aiRequest =
                assembler.assemble(request);

        assertThat(aiRequest.getWeatherTimeline()).hasSize(1);
        assertThat(aiRequest.getWeatherTimeline().get(0).getTime())
                .isEqualTo("08:00");
        assertThat(aiRequest.getWeatherTimeline().get(0).getCondition())
                .isEqualTo("rain");

        assertThat(aiRequest.getCandidatePlaces()).hasSize(1);
        assertThat(aiRequest.getCandidatePlaces().get(0).getPlaceId())
                .isEqualTo("place-1");
        assertThat(aiRequest.getCandidatePlaces().get(0).getCategory())
                .isEqualTo("landmark");
        assertThat(aiRequest.getCandidatePlaces().get(0).getLatitude())
                .isEqualTo(36.65);
    }

    @Test
    void keepsProvidedWeatherTimelineAndCandidatePlaces() {
        RouteRecommendationRequest request = createRequest();
        request.setWeatherTimeline(List.of(
                java.util.Map.of(
                        "time", "10:00",
                        "condition", "clear",
                        "precipitationProbability", 10,
                        "temperature", 25,
                        "feelsLikeTemperature", 26,
                        "fineDustLevel", "normal"
                )
        ));
        request.setCandidatePlaces(List.of(
                java.util.Map.ofEntries(
                        java.util.Map.entry("placeId", "provided-1"),
                        java.util.Map.entry("name", "Provided Place"),
                        java.util.Map.entry("category", "food"),
                        java.util.Map.entry("interests", List.of("food")),
                        java.util.Map.entry("indoor", true),
                        java.util.Map.entry("latitude", 36.7),
                        java.util.Map.entry("longitude", 127.5)
                )
        ));

        AiRouteRecommendationRequest aiRequest =
                assembler.assemble(request);

        assertThat(aiRequest.getWeatherTimeline()).hasSize(1);
        assertThat(aiRequest.getWeatherTimeline().get(0).getTime())
                .isEqualTo("10:00");
        assertThat(aiRequest.getCandidatePlaces()).hasSize(1);
        assertThat(aiRequest.getCandidatePlaces().get(0).getPlaceId())
                .isEqualTo("provided-1");
        assertThat(aiRequest.getCandidatePlaces().get(0).getCategory())
                .isEqualTo("restaurant");
    }

    @Test
    void createsFallbackDataWhenWeatherOrPlaceServiceFails() {
        RouteRecommendationRequest request = createRequest();

        when(weatherService.getRegionTimeSlotWeather(any()))
                .thenThrow(new RuntimeException("weather failed"));
        when(placeSearchService.search(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("place failed"));

        AiRouteRecommendationRequest aiRequest =
                assembler.assemble(request);

        assertThat(aiRequest.getWeatherTimeline()).hasSize(1);
        assertThat(aiRequest.getWeatherTimeline().get(0).getTime())
                .isEqualTo("09:00");
        assertThat(aiRequest.getCandidatePlaces()).hasSize(1);
        assertThat(aiRequest.getCandidatePlaces().get(0).getPlaceId())
                .isEqualTo("fallback-landmark-1");
    }

    private RouteRecommendationRequest createRequest() {
        RouteRecommendationRequest request =
                new RouteRecommendationRequest();

        request.setRegion("Cheongju");
        request.setInterests(List.of("nature"));
        request.setCompanionType("friends");
        request.setBudget("medium");
        request.setActivityIntensity("medium");
        request.setTransportMode("car");
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
                                .placeId("place-1")
                                .name("Sangdang Sanseong")
                                .address("Cheongju, Chungbuk")
                                .latitude(36.65)
                                .longitude(127.49)
                                .category("tourist attraction")
                                .primaryType("tourist_attraction")
                                .primaryTypeName("Tourist Attraction")
                                .types(List.of("tourist_attraction"))
                                .rating(4.5)
                                .userRatingCount(100)
                                .photoName("places/place-1/photos/photo-1")
                                .googleMapsUri("https://maps.example/place-1")
                                .build()
                ))
                .size(1)
                .nextPageToken(null)
                .build();
    }
}
