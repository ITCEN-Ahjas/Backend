package com.example.Chungbuk.domain.recommend.service;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import com.example.Chungbuk.domain.place.service.PlaceSearchService;
import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationMapper;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RouteRecommendationDataAssembler {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_PLACE_SEARCH_SIZE = 5;
    private static final int DEFAULT_PLACE_PHOTO_WIDTH = 320;

    private final WeatherService weatherService;
    private final PlaceSearchService placeSearchService;
    private final RouteRecommendationMapper routeRecommendationMapper;

    public RouteRecommendationDataAssembler(
            WeatherService weatherService,
            PlaceSearchService placeSearchService,
            RouteRecommendationMapper routeRecommendationMapper
    ) {
        this.weatherService = weatherService;
        this.placeSearchService = placeSearchService;
        this.routeRecommendationMapper = routeRecommendationMapper;
    }

    public AiRouteRecommendationRequest assemble(
            RouteRecommendationRequest request
    ) {
        AiRouteRecommendationRequest mappedRequest =
                routeRecommendationMapper.toAiRequest(request);

        List<AiRouteRecommendationRequest.HourlyWeather> weatherTimeline =
                mappedRequest.getWeatherTimeline();

        if (weatherTimeline == null || weatherTimeline.isEmpty()) {
            weatherTimeline = loadWeatherTimeline(request);
        }

        List<AiRouteRecommendationRequest.CandidatePlace> candidatePlaces =
                mappedRequest.getCandidatePlaces();

        if (candidatePlaces == null || candidatePlaces.isEmpty()) {
            candidatePlaces = loadCandidatePlaces(request);
        }

        return AiRouteRecommendationRequest.builder()
                .region(mappedRequest.getRegion())
                .preference(mappedRequest.getPreference())
                .constraint(mappedRequest.getConstraint())
                .weatherTimeline(weatherTimeline)
                .candidatePlaces(candidatePlaces)
                .build();
    }

    private List<AiRouteRecommendationRequest.HourlyWeather>
    loadWeatherTimeline(RouteRecommendationRequest request) {
        try {
            RegionWeatherRequest weatherRequest =
                    new RegionWeatherRequest();
            weatherRequest.setRegion(resolveRegion(request));

            RegionTimeSlotWeatherResponse response =
                    weatherService.getRegionTimeSlotWeather(weatherRequest);

            List<AiRouteRecommendationRequest.HourlyWeather> result =
                    response.getTimeSlots().stream()
                            .map(this::toHourlyWeather)
                            .toList();

            if (!result.isEmpty()) {
                return result;
            }
        } catch (RuntimeException ignored) {
            return createFallbackWeatherTimeline(request);
        }

        return createFallbackWeatherTimeline(request);
    }

    private AiRouteRecommendationRequest.HourlyWeather toHourlyWeather(
            TimeSlotWeatherResponse timeSlot
    ) {
        CurrentWeatherResponse currentWeather =
                timeSlot.getCurrentWeather();
        FeelsLikeWeatherResponse feelsLikeWeather =
                timeSlot.getFeelsLikeWeather();

        return AiRouteRecommendationRequest.HourlyWeather.builder()
                .time(formatTime(timeSlot.getStartTime()))
                .condition(resolveWeatherCondition(currentWeather))
                .precipitationProbability(
                        currentWeather.getPrecipitationProbability()
                )
                .temperature(currentWeather.getTemperature())
                .feelsLikeTemperature(
                        feelsLikeWeather.getFeelsLikeTemperature()
                )
                .fineDustLevel("normal")
                .build();
    }

    private List<AiRouteRecommendationRequest.HourlyWeather>
    createFallbackWeatherTimeline(RouteRecommendationRequest request) {
        return List.of(
                AiRouteRecommendationRequest.HourlyWeather.builder()
                        .time(resolveStartTime(request))
                        .condition("clear")
                        .precipitationProbability(0)
                        .temperature(24.0)
                        .feelsLikeTemperature(24.0)
                        .fineDustLevel("normal")
                        .build()
        );
    }

    private List<AiRouteRecommendationRequest.CandidatePlace>
    loadCandidatePlaces(RouteRecommendationRequest request) {
        Map<String, AiRouteRecommendationRequest.CandidatePlace> places =
                new LinkedHashMap<>();

        for (PlaceCategory category : resolvePlaceCategories(request)) {
            try {
                PlaceSearchResponse response = placeSearchService.search(
                        resolveRegion(request),
                        category,
                        DEFAULT_PLACE_SEARCH_SIZE,
                        null
                );

                if (response == null || response.getItems() == null) {
                    continue;
                }

                for (PlaceSummaryResponse place : response.getItems()) {
                    AiRouteRecommendationRequest.CandidatePlace candidate =
                            toCandidatePlace(place, category);
                    places.putIfAbsent(candidate.getPlaceId(), candidate);
                }
            } catch (RuntimeException ignored) {
                return createFallbackCandidatePlaces(request);
            }
        }

        if (places.isEmpty()) {
            return createFallbackCandidatePlaces(request);
        }

        return new ArrayList<>(places.values());
    }

    private AiRouteRecommendationRequest.CandidatePlace toCandidatePlace(
            PlaceSummaryResponse place,
            PlaceCategory sourceCategory
    ) {
        String category = resolvePlaceCategory(place, sourceCategory);

        return AiRouteRecommendationRequest.CandidatePlace.builder()
                .placeId(firstNonBlank(place.getPlaceId(), place.getName()))
                .name(firstNonBlank(place.getName(), "Candidate Place"))
                .category(category)
                .interests(List.of(toInterest(category)))
                .indoor(isIndoorCategory(category))
                .address(place.getAddress())
                .imageUrl(createPlacePhotoUrl(place.getPhotoName()))
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .averageStayMinutes(resolveStayMinutes(category))
                .openTime("09:00")
                .closeTime("20:00")
                .build();
    }

    private List<AiRouteRecommendationRequest.CandidatePlace>
    createFallbackCandidatePlaces(RouteRecommendationRequest request) {
        String region = routeRecommendationMapper.toAiRequest(request)
                .getRegion();

        return List.of(
                AiRouteRecommendationRequest.CandidatePlace.builder()
                        .placeId("fallback-landmark-1")
                        .name(region + " Landmark")
                        .category("landmark")
                        .interests(List.of("nature"))
                        .indoor(false)
                        .address(region)
                        .imageUrl(null)
                        .latitude(null)
                        .longitude(null)
                        .averageStayMinutes(90)
                        .openTime("09:00")
                        .closeTime("20:00")
                        .build()
        );
    }

    private List<PlaceCategory> resolvePlaceCategories(
            RouteRecommendationRequest request
    ) {
        if (request == null
                || request.getInterests() == null
                || request.getInterests().isEmpty()) {
            return List.of(PlaceCategory.TOURIST_ATTRACTION);
        }

        List<PlaceCategory> categories = new ArrayList<>();

        for (String interest : request.getInterests()) {
            PlaceCategory category = switch (trimToEmpty(interest)) {
                case "food", "restaurant", "cafe" -> PlaceCategory.RESTAURANT;
                case "shopping" -> PlaceCategory.SHOPPING;
                default -> PlaceCategory.TOURIST_ATTRACTION;
            };

            if (!categories.contains(category)) {
                categories.add(category);
            }
        }

        if (categories.isEmpty()) {
            categories.add(PlaceCategory.TOURIST_ATTRACTION);
        }

        return categories;
    }

    private String resolvePlaceCategory(
            PlaceSummaryResponse place,
            PlaceCategory sourceCategory
    ) {
        if (place.getPrimaryType() != null) {
            String primaryType = place.getPrimaryType();

            if (primaryType.contains("restaurant")) {
                return "restaurant";
            }

            if (primaryType.contains("cafe")) {
                return "cafe";
            }

            if (primaryType.contains("museum")) {
                return "museum";
            }

            if (primaryType.contains("shopping")) {
                return "shopping";
            }
        }

        return switch (sourceCategory) {
            case RESTAURANT -> "restaurant";
            case SHOPPING -> "shopping";
            default -> "landmark";
        };
    }

    private String resolveWeatherCondition(
            CurrentWeatherResponse currentWeather
    ) {
        if (currentWeather.getPrecipitationProbability() >= 60) {
            return "rain";
        }

        if (currentWeather.getTemperature() >= 30) {
            return "heat";
        }

        if (currentWeather.getTemperature() <= 0) {
            return "cold";
        }

        String condition = trimToEmpty(currentWeather.getWeatherCondition())
                .toLowerCase();

        if (condition.contains("rain")) {
            return "rain";
        }

        if (condition.contains("snow")) {
            return "snow";
        }

        if (condition.contains("cloud")) {
            return "cloudy";
        }

        return "clear";
    }

    private String resolveRegion(RouteRecommendationRequest request) {
        if (request == null
                || request.getRegion() == null
                || request.getRegion().isBlank()) {
            return "Chungbuk";
        }

        return request.getRegion().trim();
    }

    private String resolveStartTime(RouteRecommendationRequest request) {
        if (request == null
                || request.getStartTime() == null
                || request.getStartTime().isBlank()) {
            return "09:00";
        }

        return request.getStartTime().trim();
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    private String toInterest(String category) {
        return switch (category) {
            case "restaurant" -> "food";
            case "cafe" -> "cafe";
            case "museum" -> "exhibition";
            case "shopping" -> "shopping";
            case "festival" -> "festival";
            default -> "nature";
        };
    }

    private boolean isIndoorCategory(String category) {
        return switch (category) {
            case "restaurant", "cafe", "museum", "shopping" -> true;
            default -> false;
        };
    }

    private int resolveStayMinutes(String category) {
        return switch (category) {
            case "restaurant" -> 80;
            case "cafe" -> 60;
            case "museum" -> 90;
            default -> 90;
        };
    }

    private String createPlacePhotoUrl(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return null;
        }

        return UriComponentsBuilder.fromPath("/api/places/photo")
                .queryParam("name", photoName)
                .queryParam("maxWidthPx", DEFAULT_PLACE_PHOTO_WIDTH)
                .build()
                .encode()
                .toUriString();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
