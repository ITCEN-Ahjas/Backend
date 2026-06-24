package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.global.config.WeatherApiProperties;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KmaWeatherClientTest {

    private MockRestServiceServer mockServer;
    private KmaWeatherClient kmaWeatherClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();

        mockServer = MockRestServiceServer
                .bindTo(restTemplate)
                .build();

        WeatherApiProperties weatherApiProperties =
                new WeatherApiProperties();

        weatherApiProperties.setServiceKey("test-service-key");
        weatherApiProperties.setBaseUrl("https://example.com/weather");

        kmaWeatherClient = new KmaWeatherClient(
                restTemplate,
                weatherApiProperties
        );
    }

    @Test
    @DisplayName("초단기실황 응답에서 관측 날씨 항목을 추출한다")
    void getUltraSrtNcst_returnsWeatherItems() {
        mockServer.expect(request -> {
                    String requestUrl = request.getURI().toString();

                    assertTrue(
                            requestUrl.contains("getUltraSrtNcst")
                    );
                    assertTrue(requestUrl.contains("nx=69"));
                    assertTrue(requestUrl.contains("ny=107"));
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                successNowcastResponse(),
                                MediaType.APPLICATION_JSON
                        )
                );

        List<KmaWeatherItem> weatherItems =
                kmaWeatherClient.getUltraSrtNcst(
                        ChungbukRegion.CHEONGJU
                );

        assertEquals(2, weatherItems.size());
        assertEquals("T1H", weatherItems.get(0).getCategory());
        assertEquals("22.4", weatherItems.get(0).getObsrValue());
        assertEquals("REH", weatherItems.get(1).getCategory());
        assertEquals("63", weatherItems.get(1).getObsrValue());

        mockServer.verify();
    }

    @Test
    @DisplayName("단기예보 응답에서 시간대별 예보 항목을 추출한다")
    void getVilageFcst_returnsForecastItems() {
        mockServer.expect(request -> {
                    String requestUrl = request.getURI().toString();

                    assertTrue(requestUrl.contains("getVilageFcst"));
                    assertTrue(requestUrl.contains("nx=69"));
                    assertTrue(requestUrl.contains("ny=107"));
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                successVillageForecastResponse(),
                                MediaType.APPLICATION_JSON
                        )
                );

        List<KmaWeatherItem> weatherItems =
                kmaWeatherClient.getVilageFcst(
                        ChungbukRegion.CHEONGJU
                );

        assertEquals(2, weatherItems.size());
        assertEquals("TMP", weatherItems.get(0).getCategory());
        assertEquals("21", weatherItems.get(0).getFcstValue());
        assertEquals("SKY", weatherItems.get(1).getCategory());
        assertEquals("3", weatherItems.get(1).getFcstValue());

        mockServer.verify();
    }

    @Test
    @DisplayName("기상청 API 오류 응답은 예외 처리한다")
    void getUltraSrtNcst_throwsExceptionWhenKmaResponseFails() {
        mockServer.expect(request -> {
                })
                .andRespond(
                        withSuccess(
                                failedKmaResponse(),
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThrows(
                KmaWeatherApiException.class,
                () -> kmaWeatherClient.getUltraSrtNcst(
                        ChungbukRegion.CHEONGJU
                )
        );

        mockServer.verify();
    }

    private String successNowcastResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                      "dataType": "JSON",
                      "items": {
                        "item": [
                          {
                            "baseDate": "20260621",
                            "baseTime": "1200",
                            "category": "T1H",
                            "nx": 69,
                            "ny": 107,
                            "obsrValue": "22.4"
                          },
                          {
                            "baseDate": "20260621",
                            "baseTime": "1200",
                            "category": "REH",
                            "nx": 69,
                            "ny": 107,
                            "obsrValue": "63"
                          }
                        ]
                      },
                      "pageNo": 1,
                      "numOfRows": 1000,
                      "totalCount": 2
                    }
                  }
                }
                """;
    }

    private String successVillageForecastResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                      "dataType": "JSON",
                      "items": {
                        "item": [
                          {
                            "baseDate": "20260621",
                            "baseTime": "0800",
                            "category": "TMP",
                            "fcstDate": "20260621",
                            "fcstTime": "1200",
                            "fcstValue": "21"
                          },
                          {
                            "baseDate": "20260621",
                            "baseTime": "0800",
                            "category": "SKY",
                            "fcstDate": "20260621",
                            "fcstTime": "1200",
                            "fcstValue": "3"
                          }
                        ]
                      },
                      "pageNo": 1,
                      "numOfRows": 1000,
                      "totalCount": 2
                    }
                  }
                }
                """;
    }

    private String failedKmaResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "30",
                      "resultMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"
                    }
                  }
                }
                """;
    }
}
