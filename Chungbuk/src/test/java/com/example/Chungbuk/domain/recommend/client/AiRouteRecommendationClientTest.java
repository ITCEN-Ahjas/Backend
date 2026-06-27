package com.example.Chungbuk.domain.recommend.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.global.config.AiRouteRecommendationProperties;
import com.example.Chungbuk.global.exception.AiRouteRecommendationApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class AiRouteRecommendationClientTest {

    @Test
    void aiRouteResponseJsonMapsToResponseDto() throws Exception {
        String json = """
                {
                  "region": "Cheongju",
                  "source": "fallback",
                  "summary": "Weather-aware Cheongju route.",
                  "routeOverview": {
                    "title": "Cheongju weather-aware travel route",
                    "region": "Cheongju",
                    "totalPlaces": 2,
                    "totalStayMinutes": 170,
                    "startLocation": "Cheongju Station",
                    "endLocation": "Cheongju Station",
                    "styleTags": ["balanced", "public_transport"],
                    "weatherSummary": "Clear morning and rainy afternoon."
                  },
                  "itinerary": [
                    {
                      "day": 1,
                      "order": 1,
                      "placeId": "nature-1",
                      "name": "Sangdang Sanseong",
                      "category": "nature",
                      "startTime": "09:00",
                      "endTime": "10:30",
                      "indoor": false,
                      "address": "Cheongju, Chungbuk",
                      "imageUrl": "https://example.com/sangdang.jpg",
                      "latitude": 36.652,
                      "longitude": 127.492,
                      "recommendationReason": "Matches nature preference.",
                      "weatherReason": "Good outdoor weather in the morning.",
                      "moveTip": "Check bus intervals."
                    }
                  ],
                  "planB": [
                    {
                      "triggerCondition": "rain",
                      "replaceFrom": "Sangdang Sanseong",
                      "replaceTo": "Cheongju Museum",
                      "reason": "Indoor route is safer during rain."
                    }
                  ],
                  "weatherNotes": [
                    {
                      "timeRange": "12:00-15:00",
                      "summary": "Rain is likely.",
                      "cautionLevel": "medium"
                    }
                  ]
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();

        AiRouteRecommendationResponse response =
                objectMapper.readValue(
                        json,
                        AiRouteRecommendationResponse.class
                );

        assertEquals("Cheongju", response.getRegion());
        assertEquals("fallback", response.getSource());
        assertEquals(2, response.getRouteOverview().getTotalPlaces());
        assertEquals(1, response.getItinerary().get(0).getDay());
        assertEquals(36.652, response.getItinerary().get(0).getLatitude());
        assertEquals("rain", response.getPlanB().get(0).getTriggerCondition());
        assertEquals("medium", response.getWeatherNotes().get(0).getCautionLevel());
    }

    @Test
    void recommendReturnsAiResponseWhenAiServerRespondsNormally() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AiRouteRecommendationProperties properties = createProperties();
        AiRouteRecommendationResponse expectedResponse = createResponse();

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiRouteRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        AiRouteRecommendationClient client =
                new AiRouteRecommendationClient(restTemplate, properties);

        AiRouteRecommendationResponse actualResponse =
                client.recommend(createRequest());

        assertNotNull(actualResponse);
        assertEquals("Cheongju", actualResponse.getRegion());
        assertEquals("fallback", actualResponse.getSource());
    }

    @Test
    void recommendThrowsExceptionWhenAiServerConnectionFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AiRouteRecommendationProperties properties = createProperties();

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AiRouteRecommendationResponse.class)
        )).thenThrow(new RestClientException("connection failed"));

        AiRouteRecommendationClient client =
                new AiRouteRecommendationClient(restTemplate, properties);

        assertThrows(
                AiRouteRecommendationApiException.class,
                () -> client.recommend(createRequest())
        );
    }

    private AiRouteRecommendationProperties createProperties() {
        AiRouteRecommendationProperties properties =
                new AiRouteRecommendationProperties();

        properties.setBaseUrl("http://localhost:8000");
        properties.setRoutesPath("/api/v1/recommend/routes");

        return properties;
    }

    private AiRouteRecommendationRequest createRequest() {
        return AiRouteRecommendationRequest.builder()
                .region("Cheongju")
                .preference(AiRouteRecommendationRequest.Preference.builder()
                        .interests(List.of("nature", "food"))
                        .companionType("couple")
                        .budgetLevel("medium")
                        .activityPace("balanced")
                        .transportMode("public_transport")
                        .build())
                .constraint(AiRouteRecommendationRequest.Constraint.builder()
                        .travelDate("2026-06-24")
                        .startTime("09:00")
                        .endTime("18:00")
                        .startLocation("Cheongju Station")
                        .endLocation("Cheongju Station")
                        .build())
                .weatherTimeline(List.of(
                        AiRouteRecommendationRequest.HourlyWeather.builder()
                                .time("09:00")
                                .condition("clear")
                                .precipitationProbability(10)
                                .temperature(23)
                                .feelsLikeTemperature(24)
                                .fineDustLevel("normal")
                                .build()
                ))
                .candidatePlaces(List.of(
                        AiRouteRecommendationRequest.CandidatePlace.builder()
                                .placeId("nature-1")
                                .name("Sangdang Sanseong")
                                .category("nature")
                                .interests(List.of("nature"))
                                .indoor(false)
                                .address("Cheongju, Chungbuk")
                                .latitude(36.652)
                                .longitude(127.492)
                                .averageStayMinutes(90)
                                .openTime("09:00")
                                .closeTime("20:00")
                                .build()
                ))
                .build();
    }

    private AiRouteRecommendationResponse createResponse() {
        AiRouteRecommendationResponse response =
                new AiRouteRecommendationResponse();

        response.setRegion("Cheongju");
        response.setSource("fallback");
        response.setSummary("Weather-aware Cheongju route.");

        return response;
    }
}
