package com.example.Chungbuk.domain.recommend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteRecommendationMapperTest {

    private final RouteRecommendationMapper mapper =
            new RouteRecommendationMapper();

    @Test
    void mapsFrontendRequestToAiRequestShape() {
        RouteRecommendationRequest request = createRequest();

        AiRouteRecommendationRequest aiRequest =
                mapper.toAiRequest(request);

        assertThat(aiRequest.getRegion()).isEqualTo("Cheongju");
        assertThat(aiRequest.getPreference().getInterests())
                .containsExactly("nature", "food");
        assertThat(aiRequest.getPreference().getCompanionType())
                .isEqualTo("friends");
        assertThat(aiRequest.getPreference().getBudgetLevel())
                .isEqualTo("medium");
        assertThat(aiRequest.getPreference().getActivityPace())
                .isEqualTo("balanced");
        assertThat(aiRequest.getPreference().getTransportMode())
                .isEqualTo("public_transport");
        assertThat(aiRequest.getConstraint().getTravelDate())
                .isEqualTo("2026-06-24");
        assertThat(aiRequest.getConstraint().getStartTime())
                .isEqualTo("09:00");
        assertThat(aiRequest.getConstraint().getEndTime())
                .isEqualTo("18:00");
    }

    @Test
    void mapsActivityIntensityToAiActivityPace() {
        RouteRecommendationRequest request = createRequest();

        request.setActivityIntensity("low");
        assertThat(mapper.toAiRequest(request)
                .getPreference()
                .getActivityPace())
                .isEqualTo("relaxed");

        request.setActivityIntensity("medium");
        assertThat(mapper.toAiRequest(request)
                .getPreference()
                .getActivityPace())
                .isEqualTo("balanced");

        request.setActivityIntensity("high");
        assertThat(mapper.toAiRequest(request)
                .getPreference()
                .getActivityPace())
                .isEqualTo("tight");
    }

    @Test
    void mapsWeatherTimelineValues() {
        RouteRecommendationRequest request = createRequest();

        AiRouteRecommendationRequest aiRequest =
                mapper.toAiRequest(request);

        AiRouteRecommendationRequest.HourlyWeather weather =
                aiRequest.getWeatherTimeline().get(0);

        assertThat(weather.getTime()).isEqualTo("09:00");
        assertThat(weather.getCondition()).isEqualTo("rain");
        assertThat(weather.getPrecipitationProbability()).isEqualTo(80);
        assertThat(weather.getTemperature()).isEqualTo(24.0);
        assertThat(weather.getFeelsLikeTemperature()).isEqualTo(25.0);
        assertThat(weather.getFineDustLevel()).isEqualTo("normal");
    }

    @Test
    void mapsCandidatePlaceValuesAndNormalizesFoodCategory() {
        RouteRecommendationRequest request = createRequest();

        AiRouteRecommendationRequest aiRequest =
                mapper.toAiRequest(request);

        AiRouteRecommendationRequest.CandidatePlace place =
                aiRequest.getCandidatePlaces().get(0);

        assertThat(place.getPlaceId()).isEqualTo("food-1");
        assertThat(place.getName()).isEqualTo("Local Restaurant");
        assertThat(place.getCategory()).isEqualTo("restaurant");
        assertThat(place.getInterests()).containsExactly("food");
        assertThat(place.isIndoor()).isTrue();
        assertThat(place.getAddress()).isEqualTo("Cheongju, Chungbuk");
        assertThat(place.getLatitude()).isEqualTo(36.65);
        assertThat(place.getLongitude()).isEqualTo(127.49);
        assertThat(place.getAverageStayMinutes()).isEqualTo(80);
    }

    @Test
    void usesDefaultsWhenOptionalValuesAreMissing() {
        AiRouteRecommendationRequest aiRequest =
                mapper.toAiRequest(new RouteRecommendationRequest());

        assertThat(aiRequest.getRegion()).isEqualTo("Chungbuk");
        assertThat(aiRequest.getPreference().getInterests())
                .containsExactly("nature");
        assertThat(aiRequest.getPreference().getCompanionType())
                .isEqualTo("friends");
        assertThat(aiRequest.getPreference().getBudgetLevel())
                .isEqualTo("medium");
        assertThat(aiRequest.getPreference().getActivityPace())
                .isEqualTo("balanced");
        assertThat(aiRequest.getPreference().getTransportMode())
                .isEqualTo("car");
        assertThat(aiRequest.getWeatherTimeline()).isEmpty();
        assertThat(aiRequest.getCandidatePlaces()).isEmpty();
    }

    private RouteRecommendationRequest createRequest() {
        RouteRecommendationRequest request =
                new RouteRecommendationRequest();

        request.setRegion("\uCCAD\uC8FC");
        request.setInterests(List.of("nature", "food"));
        request.setCompanionType("friends");
        request.setBudget("medium");
        request.setActivityIntensity("medium");
        request.setTransportMode("publicTransit");
        request.setTravelDate("2026-06-24");
        request.setStartTime("09:00");
        request.setEndTime("18:00");
        request.setStartLocation("Cheongju Station");
        request.setEndLocation("Cheongju Station");
        request.setWeatherTimeline(List.of(
                Map.of(
                        "time", "09:00",
                        "condition", "rain",
                        "precipitationProbability", 80,
                        "temperature", 24,
                        "feelsLikeTemperature", 25,
                        "fineDustLevel", "normal"
                )
        ));
        request.setCandidatePlaces(List.of(
                Map.ofEntries(
                        Map.entry("placeId", "food-1"),
                        Map.entry("name", "Local Restaurant"),
                        Map.entry("category", "food"),
                        Map.entry("interests", List.of("food")),
                        Map.entry("indoor", true),
                        Map.entry("address", "Cheongju, Chungbuk"),
                        Map.entry("latitude", 36.65),
                        Map.entry("longitude", 127.49),
                        Map.entry("averageStayMinutes", 80),
                        Map.entry("openTime", "09:00"),
                        Map.entry("closeTime", "20:00")
                )
        ));

        return request;
    }
}
