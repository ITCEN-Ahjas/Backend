package com.example.Chungbuk.domain.place.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.place.client.GooglePlacesClient;
import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlaceSearchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchCreatesChungbukRestaurantRequestAndMapsResponse() throws Exception {
        GooglePlacesClient googlePlacesClient = mock(GooglePlacesClient.class);
        PlaceSearchService service = new PlaceSearchService(googlePlacesClient);
        GooglePlacesTextSearchResponse googleResponse = objectMapper.readValue(
                """
                {
                  "places": [
                    {
                      "id": "place-1",
                      "displayName": {
                        "text": "충북 식당",
                        "languageCode": "ko"
                      },
                      "formattedAddress": "충청북도 청주시 상당구",
                      "location": {
                        "latitude": 36.635,
                        "longitude": 127.491
                      },
                      "primaryType": "restaurant",
                      "primaryTypeDisplayName": {
                        "text": "음식점",
                        "languageCode": "ko"
                      },
                      "types": ["restaurant", "food"],
                      "rating": 4.5,
                      "userRatingCount": 120,
                      "photos": [
                        {
                          "name": "places/place-1/photos/photo-1",
                          "widthPx": 1200,
                          "heightPx": 800
                        }
                      ],
                      "googleMapsUri": "https://maps.google.com/place-1"
                    }
                  ],
                  "nextPageToken": "next-token"
                }
                """,
                GooglePlacesTextSearchResponse.class
        );
        when(googlePlacesClient.searchText(any())).thenReturn(googleResponse);

        PlaceSearchResponse response = service.search(
                "한식",
                PlaceCategory.RESTAURANT,
                30,
                " page-token "
        );

        ArgumentCaptor<GooglePlacesTextSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(GooglePlacesTextSearchRequest.class);
        verify(googlePlacesClient).searchText(requestCaptor.capture());

        GooglePlacesTextSearchRequest request = requestCaptor.getValue();
        assertEquals("한식 충청북도", request.getTextQuery());
        assertEquals("restaurant", request.getIncludedType());
        assertEquals(false, request.getStrictTypeFiltering());
        assertEquals("ko", request.getLanguageCode());
        assertEquals("KR", request.getRegionCode());
        assertEquals(20, request.getPageSize());
        assertEquals("page-token", request.getPageToken());
        assertEquals(
                35.97,
                request.getLocationRestriction().getRectangle().getLow().getLatitude()
        );
        assertEquals(
                128.65,
                request.getLocationRestriction().getRectangle().getHigh().getLongitude()
        );

        assertEquals(1, response.getSize());
        assertEquals("next-token", response.getNextPageToken());

        PlaceSummaryResponse place = response.getItems().getFirst();
        assertEquals("place-1", place.getPlaceId());
        assertEquals("충북 식당", place.getName());
        assertEquals("충청북도 청주시 상당구", place.getAddress());
        assertEquals(36.635, place.getLatitude());
        assertEquals(127.491, place.getLongitude());
        assertEquals("음식점", place.getCategory());
        assertEquals("restaurant", place.getPrimaryType());
        assertEquals("음식점", place.getPrimaryTypeName());
        assertEquals(4.5, place.getRating());
        assertEquals(120, place.getUserRatingCount());
        assertEquals("places/place-1/photos/photo-1", place.getPhotoName());
        assertEquals("https://maps.google.com/place-1", place.getGoogleMapsUri());
    }

    @Test
    void searchUsesDefaultQueryAndSizeWhenConditionsAreMissing() {
        GooglePlacesClient googlePlacesClient = mock(GooglePlacesClient.class);
        PlaceSearchService service = new PlaceSearchService(googlePlacesClient);
        when(googlePlacesClient.searchText(any()))
                .thenReturn(new GooglePlacesTextSearchResponse());

        PlaceSearchResponse response = service.search(null, null, null, null);

        ArgumentCaptor<GooglePlacesTextSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(GooglePlacesTextSearchRequest.class);
        verify(googlePlacesClient).searchText(requestCaptor.capture());

        GooglePlacesTextSearchRequest request = requestCaptor.getValue();
        assertEquals("충청북도 여행 명소", request.getTextQuery());
        assertNull(request.getIncludedType());
        assertEquals(10, request.getPageSize());
        assertNull(request.getPageToken());
        assertEquals(0, response.getSize());
        assertEquals(0, response.getItems().size());
        assertNull(response.getNextPageToken());
    }

    @Test
    void searchReturnsEmptyResultWhenGoogleResponseIsNull() {
        GooglePlacesClient googlePlacesClient = mock(GooglePlacesClient.class);
        PlaceSearchService service = new PlaceSearchService(googlePlacesClient);
        when(googlePlacesClient.searchText(any())).thenReturn(null);

        PlaceSearchResponse response = service.search(
                "청남대",
                PlaceCategory.TOURIST_ATTRACTION,
                0,
                null
        );

        assertEquals(0, response.getSize());
        assertEquals(0, response.getItems().size());
        assertNull(response.getNextPageToken());
    }
}
