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
                .andExpect(jsonPath("$.weatherNotes").isArray())
                .andExpect(jsonPath("$.itinerary").isArray())
                .andExpect(jsonPath("$.itinerary[0].time").value("09:00"))
                .andExpect(jsonPath("$.itinerary[0].placeName").value("Sangdang Sanseong"))
                .andExpect(jsonPath("$.itinerary[0].description").isString())
                .andExpect(jsonPath("$.itinerary[0].weatherReason").isString())
                .andExpect(jsonPath("$.itinerary[0].moveTip").isString())
                .andExpect(jsonPath("$.planB").isArray());
    }

    private RouteRecommendationResponse createResponse() {
        return RouteRecommendationResponse.builder()
                .summary("Recommended route summary")
                .weatherNotes(List.of("Weather note"))
                .itinerary(List.of(
                        RouteRecommendationResponse.ItineraryItem.builder()
                                .time("09:00")
                                .placeName("Sangdang Sanseong")
                                .description("Recommended reason")
                                .weatherReason("Weather reason")
                                .moveTip("Move tip")
                                .build()
                ))
                .planB(List.of("Indoor alternative"))
                .build();
    }
}
