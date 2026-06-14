package com.example.Chungbuk.domain.festival.client;

import com.example.Chungbuk.global.config.TourApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TourApiClient {

    private static final String SEARCH_FESTIVAL_PATH = "/searchFestival2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";
    private static final String DETAIL_INTRO_PATH = "/detailIntro2";
    private static final String DETAIL_IMAGE_PATH = "/detailImage2";

    private static final String CHUNGBUK_AREA_CODE = "33";
    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";

    private final RestTemplate restTemplate;
    private final TourApiProperties tourApiProperties;

    public TourApiClient(
            RestTemplate restTemplate,
            TourApiProperties tourApiProperties
    ) {
        this.restTemplate = restTemplate;
        this.tourApiProperties = tourApiProperties;
    }

    public String getFestivalListRaw(
            int page,
            int size,
            String eventStartDate,
            String sigunguCode
    ) {
        UriComponentsBuilder builder = createBaseBuilder(SEARCH_FESTIVAL_PATH)
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("arrange", "A")
                .queryParam("areaCode", CHUNGBUK_AREA_CODE)
                .queryParam("eventStartDate", eventStartDate);

        if (sigunguCode != null && !sigunguCode.isBlank()) {
            builder.queryParam("sigunguCode", sigunguCode);
        }

        String url = builder
                .build()
                .encode()
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    public String getFestivalDetailCommonRaw(String contentId) {
        String url = createBaseBuilder(DETAIL_COMMON_PATH)
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", FESTIVAL_CONTENT_TYPE_ID)
                .queryParam("defaultYN", "Y")
                .queryParam("firstImageYN", "Y")
                .queryParam("overviewYN", "Y")
                .build()
                .encode()
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    public String getFestivalDetailIntroRaw(String contentId) {
        String url = createBaseBuilder(DETAIL_INTRO_PATH)
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", FESTIVAL_CONTENT_TYPE_ID)
                .build()
                .encode()
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    public String getFestivalDetailImageRaw(String contentId) {
        String url = createBaseBuilder(DETAIL_IMAGE_PATH)
                .queryParam("contentId", contentId)
                .queryParam("imageYN", "Y")
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1)
                .build()
                .encode()
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    private UriComponentsBuilder createBaseBuilder(String path) {
        return UriComponentsBuilder
                .fromUriString(tourApiProperties.getBaseUrl() + path)
                .queryParam("serviceKey", tourApiProperties.getServiceKey())
                .queryParam("MobileOS", tourApiProperties.getMobileOs())
                .queryParam("MobileApp", tourApiProperties.getMobileApp())
                .queryParam("_type", tourApiProperties.getResponseType());
    }

    public String getBaseUrl() {
        return tourApiProperties.getBaseUrl();
    }

    public String getServiceKey() {
        return tourApiProperties.getServiceKey();
    }
}