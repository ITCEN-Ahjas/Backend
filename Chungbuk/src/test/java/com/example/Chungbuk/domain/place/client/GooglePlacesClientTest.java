package com.example.Chungbuk.domain.place.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlaceDetailResponse;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.global.config.GooglePlacesProperties;
import com.example.Chungbuk.global.exception.GooglePlacesApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

class GooglePlacesClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void searchTextSendsApiKeyAndFieldMaskInHeaders() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GooglePlacesProperties properties = new GooglePlacesProperties();
        properties.setBaseUrl("https://places.googleapis.com/");
        properties.setApiKey("test-api-key");
        properties.setFieldMask("places.id,places.displayName");

        GooglePlacesClient client = new GooglePlacesClient(restTemplate, properties);
        GooglePlacesTextSearchRequest request = GooglePlacesTextSearchRequest.builder()
                .textQuery("충청북도 관광지")
                .languageCode("ko")
                .regionCode("KR")
                .pageSize(10)
                .build();
        GooglePlacesTextSearchResponse expectedResponse = new GooglePlacesTextSearchResponse();

        when(restTemplate.postForObject(
                eq("https://places.googleapis.com/v1/places:searchText"),
                org.mockito.ArgumentMatchers.<HttpEntity<GooglePlacesTextSearchRequest>>any(),
                eq(GooglePlacesTextSearchResponse.class)
        )).thenReturn(expectedResponse);

        GooglePlacesTextSearchResponse actualResponse = client.searchText(request);

        ArgumentCaptor<HttpEntity<GooglePlacesTextSearchRequest>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(
                eq("https://places.googleapis.com/v1/places:searchText"),
                entityCaptor.capture(),
                eq(GooglePlacesTextSearchResponse.class)
        );

        HttpEntity<GooglePlacesTextSearchRequest> capturedEntity = entityCaptor.getValue();
        assertEquals("test-api-key", capturedEntity.getHeaders().getFirst("X-Goog-Api-Key"));
        assertEquals(
                "places.id,places.displayName",
                capturedEntity.getHeaders().getFirst("X-Goog-FieldMask")
        );
        assertSame(request, capturedEntity.getBody());
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void searchTextConvertsRestClientExceptionToGooglePlacesApiException() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GooglePlacesProperties properties = new GooglePlacesProperties();
        properties.setBaseUrl("https://places.googleapis.com");
        properties.setApiKey("test-api-key");
        properties.setFieldMask("places.id");
        GooglePlacesClient client = new GooglePlacesClient(restTemplate, properties);
        GooglePlacesTextSearchRequest request = GooglePlacesTextSearchRequest.builder()
                .textQuery("충청북도 관광지")
                .build();

        when(restTemplate.postForObject(
                eq("https://places.googleapis.com/v1/places:searchText"),
                org.mockito.ArgumentMatchers.<HttpEntity<GooglePlacesTextSearchRequest>>any(),
                eq(GooglePlacesTextSearchResponse.class)
        )).thenThrow(new ResourceAccessException("connection failure"));

        assertThrows(
                GooglePlacesApiException.class,
                () -> client.searchText(request)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPlaceDetailSendsApiKeyAndDetailFieldMaskInHeaders() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GooglePlacesProperties properties = new GooglePlacesProperties();
        properties.setBaseUrl("https://places.googleapis.com/");
        properties.setApiKey("test-api-key");
        properties.setDetailFieldMask("id,displayName,formattedAddress");
        GooglePlacesClient client = new GooglePlacesClient(restTemplate, properties);
        GooglePlaceDetailResponse expectedResponse = new GooglePlaceDetailResponse();

        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places/place-1"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(GooglePlaceDetailResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        GooglePlaceDetailResponse actualResponse = client.getPlaceDetail("place-1");

        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://places.googleapis.com/v1/places/place-1"),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(GooglePlaceDetailResponse.class)
        );

        HttpEntity<Void> capturedEntity = entityCaptor.getValue();
        assertEquals("test-api-key", capturedEntity.getHeaders().getFirst("X-Goog-Api-Key"));
        assertEquals(
                "id,displayName,formattedAddress",
                capturedEntity.getHeaders().getFirst("X-Goog-FieldMask")
        );
        assertSame(expectedResponse, actualResponse);
    }
}
