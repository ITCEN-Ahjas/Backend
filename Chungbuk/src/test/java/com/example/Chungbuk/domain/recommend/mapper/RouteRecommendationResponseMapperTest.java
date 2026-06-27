package com.example.Chungbuk.domain.recommend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteRecommendationResponseMapperTest {

    private final RouteRecommendationResponseMapper mapper =
            new RouteRecommendationResponseMapper();

    @Test
    void mapsAiResponseToMapPlannerFrontendResponse() {
        AiRouteRecommendationResponse aiResponse = createAiResponse();

        RouteRecommendationResponse response =
                mapper.toFrontendResponse(aiResponse);

        assertThat(response.getRegion()).isEqualTo("Cheongju");
        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getSummary())
                .isEqualTo("Weather-aware Cheongju route.");
        assertThat(response.getRouteOverview().getTotalPlaces())
                .isEqualTo(2);
        assertThat(response.getRouteOverview().getStyleTags())
                .containsExactly("balanced", "car");

        RouteRecommendationResponse.ItineraryItem item =
                response.getItinerary().get(0);

        assertThat(item.getDay()).isEqualTo(1);
        assertThat(item.getOrder()).isEqualTo(1);
        assertThat(item.getPlaceId()).isEqualTo("place-1");
        assertThat(item.getTime()).isEqualTo("09:00");
        assertThat(item.getStartTime()).isEqualTo("09:00");
        assertThat(item.getEndTime()).isEqualTo("10:30");
        assertThat(item.getPlaceName()).isEqualTo("Sangdang Sanseong");
        assertThat(item.getCategory()).isEqualTo("landmark");
        assertThat(item.getAddress()).isEqualTo("Cheongju, Chungbuk");
        assertThat(item.getLatitude()).isEqualTo(36.65);
        assertThat(item.getLongitude()).isEqualTo(127.49);
        assertThat(item.getDescription())
                .isEqualTo("Matches the user's nature preference.");
        assertThat(item.getRecommendationReason())
                .isEqualTo("Matches the user's nature preference.");
        assertThat(item.getWeatherReason())
                .isEqualTo("Good weather for outdoor walking.");
        assertThat(item.getMoveTip()).isEqualTo("Move by car.");

        assertThat(response.getWeatherNotes()).hasSize(1);
        assertThat(response.getWeatherNotes().get(0))
                .contains("12:00-15:00");
        assertThat(response.getWeatherNoteDetails().get(0).getCautionLevel())
                .isEqualTo("medium");

        assertThat(response.getPlanB()).hasSize(1);
        assertThat(response.getPlanB().get(0))
                .contains("rain");
        assertThat(response.getPlanBOptions().get(0).getReplaceTo())
                .isEqualTo("Cheongju Museum");
    }

    @Test
    void mapsNullAiResponseToEmptyFallbackResponse() {
        RouteRecommendationResponse response =
                mapper.toFrontendResponse(null);

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getSummary()).isNotBlank();
        assertThat(response.getWeatherNotes()).isEmpty();
        assertThat(response.getWeatherNoteDetails()).isEmpty();
        assertThat(response.getItinerary()).isEmpty();
        assertThat(response.getPlanB()).isEmpty();
        assertThat(response.getPlanBOptions()).isEmpty();
    }

    private AiRouteRecommendationResponse createAiResponse() {
        AiRouteRecommendationResponse response =
                new AiRouteRecommendationResponse();

        response.setRegion("Cheongju");
        response.setSource("fallback");
        response.setSummary("Weather-aware Cheongju route.");

        AiRouteRecommendationResponse.RouteOverview overview =
                new AiRouteRecommendationResponse.RouteOverview();
        overview.setTitle("Cheongju route");
        overview.setRegion("Cheongju");
        overview.setTotalPlaces(2);
        overview.setTotalStayMinutes(170);
        overview.setStartLocation("Cheongju Station");
        overview.setEndLocation("Cheongju Station");
        overview.setStyleTags(List.of("balanced", "car"));
        overview.setWeatherSummary("Clear morning and rainy afternoon.");
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
        place.setImageUrl("https://example.com/place.jpg");
        place.setLatitude(36.65);
        place.setLongitude(127.49);
        place.setRecommendationReason(
                "Matches the user's nature preference."
        );
        place.setWeatherReason("Good weather for outdoor walking.");
        place.setMoveTip("Move by car.");
        response.setItinerary(List.of(place));

        AiRouteRecommendationResponse.PlanBOption planB =
                new AiRouteRecommendationResponse.PlanBOption();
        planB.setTriggerCondition("rain");
        planB.setReplaceFrom("Sangdang Sanseong");
        planB.setReplaceTo("Cheongju Museum");
        planB.setReason("Indoor route is safer.");
        response.setPlanB(List.of(planB));

        AiRouteRecommendationResponse.WeatherNote weatherNote =
                new AiRouteRecommendationResponse.WeatherNote();
        weatherNote.setTimeRange("12:00-15:00");
        weatherNote.setSummary("Rain is likely.");
        weatherNote.setCautionLevel("medium");
        response.setWeatherNotes(List.of(weatherNote));

        return response;
    }
}
