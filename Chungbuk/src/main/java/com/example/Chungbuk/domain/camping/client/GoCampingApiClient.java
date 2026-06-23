package com.example.Chungbuk.domain.camping.client;

import com.example.Chungbuk.global.config.GoCampingApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoCampingApiClient {

    private final RestTemplate restTemplate;
    private final GoCampingApiProperties goCampingApiProperties;

    private static final String CHUNGBUK_DO_NM = "충청북도";
    private static final String DEFAULT_MOBILE_OS = "ETC";
    private static final String DEFAULT_MOBILE_APP = "ChungbukTravel";
    private static final String RESPONSE_TYPE_JSON = "json";

    public String getBasedListRaw(int page, int size) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/basedList")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("doNm", CHUNGBUK_DO_NM);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    public String getImageListRaw(String contentId) {
        UriComponentsBuilder uriBuilder = createBaseUriBuilder("/imageList")
                .queryParam("contentId", contentId)
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1);

        return restTemplate.getForObject(
                uriBuilder.build(true).toUri(),
                String.class
        );
    }

    private UriComponentsBuilder createBaseUriBuilder(String path) {
        String baseUrl = goCampingApiProperties.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("serviceKey", goCampingApiProperties.getServiceKey())
                .queryParam("MobileOS", DEFAULT_MOBILE_OS)
                .queryParam("MobileApp", DEFAULT_MOBILE_APP)
                .queryParam("_type", RESPONSE_TYPE_JSON);
    }
}
