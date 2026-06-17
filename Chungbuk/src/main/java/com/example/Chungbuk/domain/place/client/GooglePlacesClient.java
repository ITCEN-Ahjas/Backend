package com.example.Chungbuk.domain.place.client;

import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.global.config.GooglePlacesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final String TEXT_SEARCH_PATH = "/v1/places:searchText";
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

        return restTemplate.postForObject(
                createTextSearchUrl(),
                httpEntity,
                GooglePlacesTextSearchResponse.class
        );
    }

    private String createTextSearchUrl() {
        String baseUrl = googlePlacesProperties.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + TEXT_SEARCH_PATH;
    }
}
