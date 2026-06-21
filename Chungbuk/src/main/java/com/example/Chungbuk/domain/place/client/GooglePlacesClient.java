package com.example.Chungbuk.domain.place.client;

import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlaceDetailResponse;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.global.exception.GooglePlacesApiException;
import com.example.Chungbuk.global.config.GooglePlacesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final String TEXT_SEARCH_PATH = "/v1/places:searchText";
    private static final String PHOTO_MEDIA_PREFIX = "/v1/";
    private static final String PHOTO_MEDIA_SUFFIX = "/media";
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";

    private final RestTemplate restTemplate;
    private final GooglePlacesProperties googlePlacesProperties;

    public GooglePlacesTextSearchResponse searchText(GooglePlacesTextSearchRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(API_KEY_HEADER, googlePlacesProperties.getApiKey());
        headers.set(FIELD_MASK_HEADER, googlePlacesProperties.getFieldMask());

        HttpEntity<GooglePlacesTextSearchRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            return restTemplate.postForObject(
                    createTextSearchUrl(),
                    httpEntity,
                    GooglePlacesTextSearchResponse.class
            );
        } catch (RestClientException exception) {
            throw new GooglePlacesApiException("Google Places API 요청에 실패했습니다.", exception);
        }
    }

    public ResponseEntity<byte[]> getPhotoMedia(String photoName, int maxWidthPx) {
        try {
            return restTemplate.getForEntity(
                    createPhotoMediaUrl(photoName, maxWidthPx),
                    byte[].class
            );
        } catch (RestClientException exception) {
            throw new GooglePlacesApiException("Google Places API 사진 요청에 실패했습니다.", exception);
        }
    }

    public GooglePlaceDetailResponse getPlaceDetail(String placeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, googlePlacesProperties.getApiKey());
        headers.set(FIELD_MASK_HEADER, googlePlacesProperties.getDetailFieldMask());

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GooglePlaceDetailResponse> response = restTemplate.exchange(
                    createPlaceDetailUrl(placeId),
                    HttpMethod.GET,
                    httpEntity,
                    GooglePlaceDetailResponse.class
            );

            return response.getBody();
        } catch (RestClientException exception) {
            throw new GooglePlacesApiException("Google Places API 상세 요청에 실패했습니다.", exception);
        }
    }

    private String createTextSearchUrl() {
        String baseUrl = googlePlacesProperties.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + TEXT_SEARCH_PATH;
    }

    private String createPhotoMediaUrl(String photoName, int maxWidthPx) {
        return UriComponentsBuilder
                .fromUriString(createBaseUrl() + PHOTO_MEDIA_PREFIX + photoName + PHOTO_MEDIA_SUFFIX)
                .queryParam("maxWidthPx", maxWidthPx)
                .queryParam("key", googlePlacesProperties.getApiKey())
                .build(false)
                .toUriString();
    }

    private String createPlaceDetailUrl(String placeId) {
        return UriComponentsBuilder
                .fromUriString(createBaseUrl() + "/v1/places/" + placeId)
                .build(false)
                .toUriString();
    }

    private String createBaseUrl() {
        String baseUrl = googlePlacesProperties.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
