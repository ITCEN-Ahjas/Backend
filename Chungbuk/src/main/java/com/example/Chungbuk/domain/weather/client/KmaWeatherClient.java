package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.dto.response.KmaApiResponse;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.domain.weather.util.KmaBaseDateTime;
import com.example.Chungbuk.domain.weather.util.KmaDateTimeUtil;
import com.example.Chungbuk.global.config.WeatherApiProperties;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KmaWeatherClient {

    private static final int PAGE_NUMBER = 1;
    private static final int NUMBER_OF_ROWS = 1_000;

    private final RestTemplate kmaRestTemplate;
    private final WeatherApiProperties weatherApiProperties;

    public KmaWeatherClient(
            @Qualifier("kmaRestTemplate") RestTemplate kmaRestTemplate,
            WeatherApiProperties weatherApiProperties
    ) {
        this.kmaRestTemplate = kmaRestTemplate;
        this.weatherApiProperties = weatherApiProperties;
    }

    public List<KmaWeatherItem> getUltraSrtNcst(
            ChungbukRegion region
    ) {
        KmaBaseDateTime baseDateTime =
                KmaDateTimeUtil.getUltraSrtNcstBaseDateTime();

        return requestWeatherItems(
                "getUltraSrtNcst",
                region,
                baseDateTime
        );
    }

    public List<KmaWeatherItem> getUltraSrtFcst(
            ChungbukRegion region
    ) {
        KmaBaseDateTime baseDateTime =
                KmaDateTimeUtil.getUltraSrtFcstBaseDateTime();

        return requestWeatherItems(
                "getUltraSrtFcst",
                region,
                baseDateTime
        );
    }

    public List<KmaWeatherItem> getVilageFcst(
            ChungbukRegion region
    ) {
        KmaBaseDateTime baseDateTime =
                KmaDateTimeUtil.getVilageFcstBaseDateTime();

        return requestWeatherItems(
                "getVilageFcst",
                region,
                baseDateTime
        );
    }

    private List<KmaWeatherItem> requestWeatherItems(
            String endpoint,
            ChungbukRegion region,
            KmaBaseDateTime baseDateTime
    ) {
        weatherApiProperties.validateServiceKey();

        URI requestUri = createRequestUri(
                endpoint,
                region,
                baseDateTime
        );

        try {
            ResponseEntity<KmaApiResponse> responseEntity =
                    kmaRestTemplate.exchange(
                            requestUri,
                            HttpMethod.GET,
                            HttpEntity.EMPTY,
                            KmaApiResponse.class
                    );

            return extractWeatherItems(
                    responseEntity.getBody(),
                    region
            );
        } catch (RestClientException exception) {
            throw new KmaWeatherApiException(
                    "기상청 날씨 데이터를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private URI createRequestUri(
            String endpoint,
            ChungbukRegion region,
            KmaBaseDateTime baseDateTime
    ) {
        return UriComponentsBuilder
                .fromUriString(weatherApiProperties.getBaseUrl())
                .pathSegment(endpoint)
                .queryParam(
                        "serviceKey",
                        weatherApiProperties.getServiceKey()
                )
                .queryParam("pageNo", PAGE_NUMBER)
                .queryParam("numOfRows", NUMBER_OF_ROWS)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDateTime.baseDate())
                .queryParam("base_time", baseDateTime.baseTime())
                .queryParam("nx", region.getNx())
                .queryParam("ny", region.getNy())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private List<KmaWeatherItem> extractWeatherItems(
            KmaApiResponse apiResponse,
            ChungbukRegion region
    ) {
        if (apiResponse == null || apiResponse.getResponse() == null) {
            throw new KmaWeatherApiException(
                    "기상청 응답 형식이 올바르지 않습니다."
            );
        }

        KmaApiResponse.KmaHeader header =
                apiResponse.getResponse().getHeader();

        if (header == null || !"00".equals(header.getResultCode())) {
            String errorMessage = header == null
                    ? "알 수 없는 기상청 응답 오류"
                    : header.getResultMsg();

            throw new KmaWeatherApiException(
                    "기상청 API 요청에 실패했습니다: " + errorMessage
            );
        }

        KmaApiResponse.KmaBody body =
                apiResponse.getResponse().getBody();

        if (body == null
                || body.getItems() == null
                || body.getItems().getItem() == null
                || body.getItems().getItem().isEmpty()) {

            throw new KmaWeatherApiException(
                    region.getDisplayName()
                            + " 지역의 기상청 날씨 데이터가 없습니다."
            );
        }

        return body.getItems().getItem();
    }
}
