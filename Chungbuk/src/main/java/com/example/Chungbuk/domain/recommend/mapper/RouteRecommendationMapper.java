package com.example.Chungbuk.domain.recommend.mapper;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RouteRecommendationMapper {

    private static final String DEFAULT_REGION = "Chungbuk";
    private static final String DEFAULT_COMPANION_TYPE = "friends";
    private static final String DEFAULT_BUDGET_LEVEL = "medium";
    private static final String DEFAULT_ACTIVITY_PACE = "balanced";
    private static final String DEFAULT_TRANSPORT_MODE = "car";
    private static final String DEFAULT_FINE_DUST_LEVEL = "normal";
    private static final int DEFAULT_STAY_MINUTES = 90;

    public AiRouteRecommendationRequest toAiRequest(
            RouteRecommendationRequest request
    ) {
        return AiRouteRecommendationRequest.builder()
                .region(normalizeRegion(request == null ? null : request.getRegion()))
                .preference(createPreference(request))
                .constraint(createConstraint(request))
                .weatherTimeline(createWeatherTimeline(request))
                .candidatePlaces(createCandidatePlaces(request))
                .build();
    }

    private AiRouteRecommendationRequest.Preference createPreference(
            RouteRecommendationRequest request
    ) {
        return AiRouteRecommendationRequest.Preference.builder()
                .interests(normalizeInterests(request == null ? null : request.getInterests()))
                .companionType(normalizeCompanionType(request == null ? null : request.getCompanionType()))
                .budgetLevel(normalizeBudgetLevel(request == null ? null : request.getBudget()))
                .activityPace(normalizeActivityPace(request == null ? null : request.getActivityIntensity()))
                .transportMode(normalizeTransportMode(request == null ? null : request.getTransportMode()))
                .build();
    }

    private AiRouteRecommendationRequest.Constraint createConstraint(
            RouteRecommendationRequest request
    ) {
        return AiRouteRecommendationRequest.Constraint.builder()
                .travelDate(trimToNull(request == null ? null : request.getTravelDate()))
                .startTime(trimToNull(request == null ? null : request.getStartTime()))
                .endTime(trimToNull(request == null ? null : request.getEndTime()))
                .startLocation(trimToNull(request == null ? null : request.getStartLocation()))
                .endLocation(trimToNull(request == null ? null : request.getEndLocation()))
                .build();
    }

    private List<AiRouteRecommendationRequest.HourlyWeather> createWeatherTimeline(
            RouteRecommendationRequest request
    ) {
        if (request == null || request.getWeatherTimeline() == null) {
            return Collections.emptyList();
        }

        return request.getWeatherTimeline().stream()
                .filter(Objects::nonNull)
                .map(this::toHourlyWeather)
                .toList();
    }

    private AiRouteRecommendationRequest.HourlyWeather toHourlyWeather(
            Map<String, Object> source
    ) {
        return AiRouteRecommendationRequest.HourlyWeather.builder()
                .time(getString(source, "time"))
                .condition(normalizeWeatherCondition(getString(source, "condition")))
                .precipitationProbability(getInt(source, "precipitationProbability", 0))
                .temperature(getDouble(source, "temperature", 0.0))
                .feelsLikeTemperature(getDouble(source, "feelsLikeTemperature", 0.0))
                .fineDustLevel(normalizeFineDustLevel(getString(source, "fineDustLevel")))
                .build();
    }

    private List<AiRouteRecommendationRequest.CandidatePlace> createCandidatePlaces(
            RouteRecommendationRequest request
    ) {
        if (request == null || request.getCandidatePlaces() == null) {
            return Collections.emptyList();
        }

        return request.getCandidatePlaces().stream()
                .filter(Objects::nonNull)
                .map(this::toCandidatePlace)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private AiRouteRecommendationRequest.CandidatePlace toCandidatePlace(
            Map<String, Object> source
    ) {
        String category = normalizePlaceCategory(getString(source, "category"));
        List<String> interests = normalizeInterests(
                (List<String>) source.get("interests")
        );

        if (interests.isEmpty()) {
            interests = List.of(toInterestFromCategory(category));
        }

        return AiRouteRecommendationRequest.CandidatePlace.builder()
                .placeId(firstNonBlank(
                        getString(source, "placeId"),
                        getString(source, "id"),
                        getString(source, "name")
                ))
                .name(firstNonBlank(
                        getString(source, "name"),
                        getString(source, "placeName"),
                        getString(source, "title")
                ))
                .category(category)
                .interests(interests)
                .indoor(getBoolean(source, "indoor", isIndoorCategory(category)))
                .address(getString(source, "address"))
                .imageUrl(firstNonBlank(
                        getString(source, "imageUrl"),
                        getString(source, "photoUrl")
                ))
                .latitude(getNullableDouble(source, "latitude"))
                .longitude(getNullableDouble(source, "longitude"))
                .averageStayMinutes(getInt(source, "averageStayMinutes", DEFAULT_STAY_MINUTES))
                .openTime(getString(source, "openTime"))
                .closeTime(getString(source, "closeTime"))
                .build();
    }

    private String normalizeRegion(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_REGION;
        }

        return switch (normalizedValue) {
            case "\uCCAD\uC8FC" -> "Cheongju";
            case "\uCDA9\uC8FC" -> "Chungju";
            case "\uC81C\uCC9C" -> "Jecheon";
            case "\uBCF4\uC740" -> "Boeun";
            case "\uC625\uCC9C" -> "Okcheon";
            case "\uC601\uB3D9" -> "Yeongdong";
            case "\uC99D\uD3C9" -> "Jeungpyeong";
            case "\uC9C4\uCC9C" -> "Jincheon";
            case "\uAD34\uC0B0" -> "Goesan";
            case "\uC74C\uC131" -> "Eumseong";
            case "\uB2E8\uC591" -> "Danyang";
            default -> normalizedValue;
        };
    }

    private List<String> normalizeInterests(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of("nature");
        }

        return values.stream()
                .map(this::normalizeInterest)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String normalizeInterest(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return null;
        }

        return switch (normalizedValue) {
            case "restaurant" -> "food";
            case "museum" -> "exhibition";
            case "experience" -> "activity";
            case "landmark" -> "nature";
            default -> normalizedValue;
        };
    }

    private String normalizeCompanionType(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_COMPANION_TYPE;
        }

        return normalizedValue;
    }

    private String normalizeBudgetLevel(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_BUDGET_LEVEL;
        }

        return normalizedValue;
    }

    private String normalizeActivityPace(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_ACTIVITY_PACE;
        }

        return switch (normalizedValue) {
            case "low", "relaxed" -> "relaxed";
            case "high", "tight" -> "tight";
            default -> DEFAULT_ACTIVITY_PACE;
        };
    }

    private String normalizeTransportMode(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_TRANSPORT_MODE;
        }

        return switch (normalizedValue) {
            case "publicTransit", "public_transport" -> "public_transport";
            case "walk" -> "walk";
            case "taxi" -> "taxi";
            default -> DEFAULT_TRANSPORT_MODE;
        };
    }

    private String normalizeWeatherCondition(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return "clear";
        }

        return switch (normalizedValue) {
            case "clear", "cloudy", "rain", "snow", "heat", "cold", "dust" -> normalizedValue;
            default -> "clear";
        };
    }

    private String normalizeFineDustLevel(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return DEFAULT_FINE_DUST_LEVEL;
        }

        return switch (normalizedValue) {
            case "good", "normal", "bad", "very_bad" -> normalizedValue;
            default -> DEFAULT_FINE_DUST_LEVEL;
        };
    }

    private String normalizePlaceCategory(String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue == null) {
            return "landmark";
        }

        return switch (normalizedValue) {
            case "food" -> "restaurant";
            case "exhibition" -> "museum";
            case "activity" -> "experience";
            case "nature", "restaurant", "cafe", "museum", "experience",
                    "shopping", "festival", "landmark" -> normalizedValue;
            default -> "landmark";
        };
    }

    private String toInterestFromCategory(String category) {
        return switch (category) {
            case "restaurant" -> "food";
            case "museum" -> "exhibition";
            case "experience" -> "activity";
            case "cafe" -> "cafe";
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

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);

        if (value == null) {
            return null;
        }

        return trimToNull(String.valueOf(value));
    }

    private int getInt(
            Map<String, Object> source,
            String key,
            int defaultValue
    ) {
        Object value = source.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        String text = trimToNull(value == null ? null : String.valueOf(value));

        if (text == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private double getDouble(
            Map<String, Object> source,
            String key,
            double defaultValue
    ) {
        Double value = getNullableDouble(source, key);

        return value == null ? defaultValue : value;
    }

    private Double getNullableDouble(Map<String, Object> source, String key) {
        Object value = source.get(key);

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        String text = trimToNull(value == null ? null : String.valueOf(value));

        if (text == null) {
            return null;
        }

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean getBoolean(
            Map<String, Object> source,
            String key,
            boolean defaultValue
    ) {
        Object value = source.get(key);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        String text = trimToNull(value == null ? null : String.valueOf(value));

        if (text == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(text);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalizedValue = trimToNull(value);

            if (normalizedValue != null) {
                return normalizedValue;
            }
        }

        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
