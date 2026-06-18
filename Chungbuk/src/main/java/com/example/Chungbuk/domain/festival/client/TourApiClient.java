package com.example.Chungbuk.domain.festival.client;

import com.example.Chungbuk.global.config.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final String CHUNGBUK_AREA_CODE = "33";
    private static final String DEFAULT_MOBILE_OS = "ETC";
    private static final String DEFAULT_MOBILE_APP = "ChungbukTravel";
    private static final String RESPONSE_TYPE_JSON = "json";

    private final RestTemplate restTemplate;
    private final TourApiProperties tourApiProperties;

    public String getFestivalListRaw(
            int page,
            int size,
            String eventStartDate,
            String sigunguCode
    ) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/searchFestival2")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("arrange", "O")
                .queryParam("areaCode", CHUNGBUK_AREA_CODE)
                .queryParam("eventStartDate", eventStartDate);

        if (hasText(sigunguCode)) {
            uriBuilder.queryParam("sigunguCode", sigunguCode);
        }

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getExperienceListRaw(
            int page,
            int size,
            String sigunguCode,
            String contentTypeId
    ) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/areaBasedList2")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("arrange", "O")
                .queryParam("areaCode", CHUNGBUK_AREA_CODE);

        if (hasText(sigunguCode)) {
            uriBuilder.queryParam("sigunguCode", sigunguCode);
        }

        if (hasText(contentTypeId)) {
            uriBuilder.queryParam("contentTypeId", contentTypeId);
        }

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getFestivalDetailCommonRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailCommon2")
                .queryParam("contentId", contentId)
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getFestivalDetailIntroRaw(
            String contentId,
            String contentTypeId
    ) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailIntro2")
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getFestivalDetailImageRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailImage2")
                .queryParam("contentId", contentId)
                .queryParam("imageYN", "Y")
                .queryParam("subImageYN", "Y")
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    private UriComponentsBuilder createBaseUriBuilder(String path) {
        String baseUrl = tourApiProperties.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("serviceKey", tourApiProperties.getServiceKey())
                .queryParam("MobileOS", DEFAULT_MOBILE_OS)
                .queryParam("MobileApp", DEFAULT_MOBILE_APP)
                .queryParam("_type", RESPONSE_TYPE_JSON);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}