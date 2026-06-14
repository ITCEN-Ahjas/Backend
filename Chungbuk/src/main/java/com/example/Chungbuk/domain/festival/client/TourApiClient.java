package com.example.Chungbuk.domain.festival.client;

import com.example.Chungbuk.global.config.TourApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TourApiClient {

    private final RestTemplate restTemplate;
    private final TourApiProperties tourApiProperties;

    public TourApiClient(
            RestTemplate restTemplate,
            TourApiProperties tourApiProperties
    ) {
        this.restTemplate = restTemplate;
        this.tourApiProperties = tourApiProperties;
    }

    public String getBaseUrl() {
        return tourApiProperties.getBaseUrl();
    }

    public String getServiceKey() {
        return tourApiProperties.getServiceKey();
    }
}