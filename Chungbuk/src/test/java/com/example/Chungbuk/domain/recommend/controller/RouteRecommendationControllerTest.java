package com.example.Chungbuk.domain.recommend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.Chungbuk.domain.recommend.service.RouteRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RouteRecommendationControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RouteRecommendationController controller =
                new RouteRecommendationController(new RouteRecommendationService());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void recommendRoutesReturnsFrontendResponseShape() throws Exception {
        String requestBody = """
                {
                  "region": "청주",
                  "interests": ["nature"],
                  "companionType": "friends",
                  "budget": "medium",
                  "activityIntensity": "medium",
                  "transportMode": "car",
                  "travelDate": "2026-06-24",
                  "startTime": "09:00",
                  "endTime": "18:00",
                  "startLocation": "청주 시외버스터미널",
                  "endLocation": "청주 시외버스터미널",
                  "weatherTimeline": [],
                  "candidatePlaces": []
                }
                """;

        mockMvc.perform(post("/api/v1/recommend/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.weatherNotes").isArray())
                .andExpect(jsonPath("$.itinerary").isArray())
                .andExpect(jsonPath("$.itinerary[0].time").value("09:00"))
                .andExpect(jsonPath("$.itinerary[0].placeName").value("상당산성"))
                .andExpect(jsonPath("$.itinerary[0].description").isString())
                .andExpect(jsonPath("$.itinerary[0].weatherReason").isString())
                .andExpect(jsonPath("$.itinerary[0].moveTip").isString())
                .andExpect(jsonPath("$.planB").isArray());
    }
}
