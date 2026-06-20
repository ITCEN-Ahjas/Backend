package com.example.Chungbuk.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceDetailResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import com.example.Chungbuk.domain.place.service.PlaceSearchService;
import com.example.Chungbuk.global.exception.GlobalExceptionHandler;
import com.example.Chungbuk.global.exception.GooglePlacesApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlaceControllerTest {

    private PlaceSearchService placeSearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        placeSearchService = mock(PlaceSearchService.class);
        PlaceController controller = new PlaceController(placeSearchService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchPlacesReturnsSearchResult() throws Exception {
        PlaceSummaryResponse place = PlaceSummaryResponse.builder()
                .placeId("place-1")
                .name("청남대")
                .address("충청북도 청주시 상당구")
                .latitude(36.462)
                .longitude(127.491)
                .category("관광지")
                .build();
        PlaceSearchResponse response = PlaceSearchResponse.builder()
                .items(List.of(place))
                .size(1)
                .nextPageToken("next-token")
                .build();
        when(placeSearchService.search(
                "청남대",
                PlaceCategory.TOURIST_ATTRACTION,
                10,
                "page-token"
        )).thenReturn(response);

        mockMvc.perform(get("/api/places")
                        .param("keyword", "청남대")
                        .param("category", "TOURIST_ATTRACTION")
                        .param("size", "10")
                        .param("pageToken", "page-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].placeId").value("place-1"))
                .andExpect(jsonPath("$.items[0].name").value("청남대"))
                .andExpect(jsonPath("$.items[0].category").value("관광지"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.nextPageToken").value("next-token"));

        verify(placeSearchService).search(
                "청남대",
                PlaceCategory.TOURIST_ATTRACTION,
                10,
                "page-token"
        );
    }

    @Test
    void searchPlacesUsesDefaultCategoryAndSize() throws Exception {
        PlaceSearchResponse response = PlaceSearchResponse.builder()
                .items(List.of())
                .size(0)
                .nextPageToken(null)
                .build();
        when(placeSearchService.search(null, PlaceCategory.ALL, 10, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.size").value(0));

        verify(placeSearchService).search(null, PlaceCategory.ALL, 10, null);
    }

    @Test
    void searchPlacesRejectsUnsupportedCategory() throws Exception {
        mockMvc.perform(get("/api/places")
                        .param("category", "CAFE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 장소 카테고리입니다."))
                .andExpect(jsonPath("$.path").value("/api/places"));
    }

    @Test
    void searchPlacesRejectsSizeGreaterThanMaximum() throws Exception {
        mockMvc.perform(get("/api/places")
                        .param("size", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("size는 1 이상 20 이하여야 합니다."))
                .andExpect(jsonPath("$.path").value("/api/places"));
    }

    @Test
    void searchPlacesReturnsBadGatewayWhenGoogleApiFails() throws Exception {
        when(placeSearchService.search(
                any(),
                eq(PlaceCategory.ALL),
                eq(10),
                any()
        )).thenThrow(new GooglePlacesApiException(
                "Google Places API 요청에 실패했습니다.",
                new RuntimeException("connection failure")
        ));

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.code").value("GOOGLE_PLACES_API_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("장소 검색 서비스에 일시적으로 연결할 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/places"));
    }

    @Test
    void getPlaceDetailReturnsPlaceDetail() throws Exception {
        PlaceDetailResponse response = PlaceDetailResponse.builder()
                .placeId("place-1")
                .name("Gosucave")
                .address("Chungbuk Danyang")
                .rating(4.5)
                .photoName("places/place-1/photos/photo-1")
                .photoNames(List.of("places/place-1/photos/photo-1"))
                .weekdayDescriptions(List.of("Monday: 09:00-18:00"))
                .build();
        when(placeSearchService.getPlaceDetail("place-1")).thenReturn(response);

        mockMvc.perform(get("/api/places/place-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value("place-1"))
                .andExpect(jsonPath("$.name").value("Gosucave"))
                .andExpect(jsonPath("$.photoName").value("places/place-1/photos/photo-1"))
                .andExpect(jsonPath("$.weekdayDescriptions[0]").value("Monday: 09:00-18:00"));

        verify(placeSearchService).getPlaceDetail("place-1");
    }
}
