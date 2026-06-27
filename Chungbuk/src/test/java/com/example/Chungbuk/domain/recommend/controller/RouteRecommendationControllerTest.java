package com.example.Chungbuk.domain.recommend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.service.RouteRecommendationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RouteRecommendationControllerTest {

    private RouteRecommendationService routeRecommendationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        routeRecommendationService =
                mock(RouteRecommendationService.class);
        RouteRecommendationController controller =
                new RouteRecommendationController(routeRecommendationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void recommendRoutesReturnsFrontendResponseShape() throws Exception {
        String requestBody = """
                {
                  "region": "Cheongju",
                  "interests": ["nature"],
                  "companionType": "friends",
                  "budget": "medium",
                  "activityIntensity": "medium",
                  "transportMode": "car",
                  "travelDate": "2026-06-24",
                  "startTime": "09:00",
                  "endTime": "18:00",
                  "startLocation": "Cheongju Station",
                  "endLocation": "Cheongju Station",
                  "weatherTimeline": [],
                  "candidatePlaces": []
                }
                """;

        when(routeRecommendationService.recommend(any()))
                .thenReturn(createResponse());

        mockMvc.perform(post("/api/v1/recommend/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.totalDistance").value("12.4km"))
                .andExpect(jsonPath("$.totalDuration").value("3h 20min"))
                .andExpect(jsonPath("$.routeOverview.totalDistance").value("12.4km"))
                .andExpect(jsonPath("$.routeOverview.totalDuration").value("3h 20min"))
                .andExpect(jsonPath("$.weatherNotes").isArray())
                .andExpect(jsonPath("$.itinerary").isArray())
                .andExpect(jsonPath("$.itinerary[0].time").value("09:00"))
                .andExpect(jsonPath("$.itinerary[0].placeName").value("Sangdang Sanseong"))
                .andExpect(jsonPath("$.itinerary[0].address").value("Cheongju, Chungbuk"))
                .andExpect(jsonPath("$.itinerary[0].imageUrl").value("/api/places/photo?name=places/place-1/photos/photo-1&maxWidthPx=320"))
                .andExpect(jsonPath("$.itinerary[0].latitude").value(36.65))
                .andExpect(jsonPath("$.itinerary[0].longitude").value(127.49))
                .andExpect(jsonPath("$.itinerary[0].description").isString())
                .andExpect(jsonPath("$.itinerary[0].weatherReason").isString())
                .andExpect(jsonPath("$.itinerary[0].moveTip").isString())
                .andExpect(jsonPath("$.planB").isArray())
                .andExpect(jsonPath("$.planBOptions").isArray())
                .andExpect(jsonPath("$.planBOptions[0].replaceTo").value("Cheongju Museum"));
    }

    private RouteRecommendationResponse createResponse() {
        return RouteRecommendationResponse.builder()
                .summary("Recommended route summary")
                .totalDistance("12.4km")
                .totalDuration("3h 20min")
                .routeOverview(RouteRecommendationResponse.RouteOverview.builder()
                        .title("Cheongju route")
                        .region("Cheongju")
                        .totalPlaces(1)
                        .totalStayMinutes(200)
                        .totalDistance("12.4km")
                        .totalDuration("3h 20min")
                        .startLocation("Cheongju Station")
                        .endLocation("Cheongju Station")
                        .styleTags(List.of("nature", "car"))
                        .weatherSummary("Good weather for outdoor stops.")
                        .build())
                .weatherNotes(List.of("Weather note"))
                .itinerary(List.of(
                        RouteRecommendationResponse.ItineraryItem.builder()
                                .time("09:00")
                                .placeName("Sangdang Sanseong")
                                .address("Cheongju, Chungbuk")
                                .imageUrl("/api/places/photo?name=places/place-1/photos/photo-1&maxWidthPx=320")
                                .latitude(36.65)
                                .longitude(127.49)
                                .description("Recommended reason")
                                .weatherReason("Weather reason")
                                .moveTip("Move tip")
                                .build()
                ))
                .planB(List.of("Indoor alternative"))
                .planBOptions(List.of(
                        RouteRecommendationResponse.PlanBOption.builder()
                                .triggerCondition("rain")
                                .replaceFrom("Sangdang Sanseong")
                                .replaceTo("Cheongju Museum")
                                .reason("Indoor route is safer.")
                                .build()
                ))
                .build();
    }
}
