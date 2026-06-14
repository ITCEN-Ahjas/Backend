package com.example.Chungbuk.domain.festival.client;

import com.example.Chungbuk.global.config.TourApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TourApiClient {

    private static final String SEARCH_FESTIVAL_PATH = "/searchFestival2";
    private static final String CHUNGBUK_AREA_CODE = "33";

    private final RestTemplate restTemplate;
    private final TourApiProperties tourApiProperties;

    public TourApiClient(
            RestTemplate restTemplate,
            TourApiProperties tourApiProperties
    ) {
        this.restTemplate = restTemplate;
        this.tourApiProperties = tourApiProperties;
    }

    public String getFestivalListRaw(int page, int size, String eventStartDate) {
        String url = UriComponentsBuilder
                .fromUriString(tourApiProperties.getBaseUrl() + SEARCH_FESTIVAL_PATH)
                .queryParam("serviceKey", tourApiProperties.getServiceKey())
                .queryParam("MobileOS", tourApiProperties.getMobileOs())
                .queryParam("MobileApp", tourApiProperties.getMobileApp())
                .queryParam("_type", tourApiProperties.getResponseType())
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("arrange", "A")
                .queryParam("areaCode", CHUNGBUK_AREA_CODE)
                .queryParam("eventStartDate", eventStartDate)
                .build()
                .encode()
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    public String getBaseUrl() {
        return tourApiProperties.getBaseUrl();
    }

    public String getServiceKey() {
        return tourApiProperties.getServiceKey();
    }
}