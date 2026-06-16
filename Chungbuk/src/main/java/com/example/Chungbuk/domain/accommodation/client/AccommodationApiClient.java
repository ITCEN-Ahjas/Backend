package com.example.Chungbuk.domain.accommodation.client;

import com.example.Chungbuk.global.config.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AccommodationApiClient {

    private final RestTemplate restTemplate;
    private final TourApiProperties tourApiProperties;

    private static final String CHUNGBUK_AREA_CODE = "33";
    private static final String ACCOMMODATION_CONTENT_TYPE_ID = "32";
    private static final String DEFAULT_MOBILE_OS = "ETC";
    private static final String DEFAULT_MOBILE_APP = "ChungbukTravel";
    private static final String RESPONSE_TYPE_JSON = "json";

    public String getAccommodationListRaw(
            int page,
            int size,
            String sigunguCode
    ) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/areaBasedList2")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("arrange", "O")
                .queryParam("areaCode", CHUNGBUK_AREA_CODE)
                .queryParam("contentTypeId", ACCOMMODATION_CONTENT_TYPE_ID);

        if (hasText(sigunguCode)) {
            uriBuilder.queryParam("sigunguCode", sigunguCode);
        }

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getAccommodationDetailCommonRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailCommon2")
                .queryParam("contentId", contentId)
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getAccommodationDetailIntroRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailIntro2")
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", ACCOMMODATION_CONTENT_TYPE_ID)
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getAccommodationDetailImageRaw(String contentId) {
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

    public String getAccommodationRoomInfoRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/detailInfo2")
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", ACCOMMODATION_CONTENT_TYPE_ID)
                .queryParam("numOfRows", 20)
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
