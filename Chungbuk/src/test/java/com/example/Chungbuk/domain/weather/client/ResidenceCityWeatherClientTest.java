package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.config.OpenMeteoApiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResidenceCityWeatherClientTest {

    @Test
    void searchCities_callsGeocodingApiWithCountryCode() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.getForEntity(
                any(URI.class),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoGeocodingResponse.class)
        )).thenReturn(ResponseEntity.ok(createTokyoGeocodingResponse()));

        ResidenceCityWeatherClient client =
                new ResidenceCityWeatherClient(
                        restTemplate,
                        createProperties()
                );

        List<ResidenceCitySearchResponse> cities = client.searchCities(
                "JP",
                "Tokyo"
        );

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        verify(restTemplate).getForEntity(
                uriCaptor.capture(),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoGeocodingResponse.class)
        );

        String requestUrl = uriCaptor.getValue().toString();

        assertTrue(requestUrl.contains("name=Tokyo"));
        assertTrue(requestUrl.contains("countryCode=JP"));
        assertEquals(1, cities.size());
        assertEquals("Tokyo", cities.get(0).getCity());
        assertEquals("JP", cities.get(0).getCountryCode());
    }

    @Test
    void searchCities_findsNewYorkFromCompactInputWithoutAliasMap() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.getForEntity(
                any(URI.class),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoGeocodingResponse.class)
        )).thenAnswer(invocation -> {
            URI requestUri = invocation.getArgument(0);
            String requestUrl = URLDecoder.decode(
                    requestUri.toString(),
                    StandardCharsets.UTF_8
            );

            if (requestUrl.contains("name=new york")) {
                return ResponseEntity.ok(createNewYorkGeocodingResponse());
            }

            return ResponseEntity.ok(createGeocodingResponse(List.of()));
        });

        ResidenceCityWeatherClient client =
                new ResidenceCityWeatherClient(
                        restTemplate,
                        createProperties()
                );

        List<ResidenceCitySearchResponse> cities = client.searchCities(
                "US",
                "newyork"
        );

        assertEquals(1, cities.size());
        assertEquals("New York", cities.get(0).getCity());
        assertEquals("US", cities.get(0).getCountryCode());
    }

    @Test
    void searchCities_findsSanFranciscoFromCompactInputWithoutCitySpecificRule() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.getForEntity(
                any(URI.class),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoGeocodingResponse.class)
        )).thenAnswer(invocation -> {
            URI requestUri = invocation.getArgument(0);
            String requestUrl = URLDecoder.decode(
                    requestUri.toString(),
                    StandardCharsets.UTF_8
            );

            if (requestUrl.contains("name=san francisco")) {
                return ResponseEntity.ok(createSanFranciscoGeocodingResponse());
            }

            return ResponseEntity.ok(createGeocodingResponse(List.of()));
        });

        ResidenceCityWeatherClient client =
                new ResidenceCityWeatherClient(
                        restTemplate,
                        createProperties()
                );

        List<ResidenceCitySearchResponse> cities = client.searchCities(
                "US",
                "SanFrancisco"
        );

        assertEquals(1, cities.size());
        assertEquals("San Francisco", cities.get(0).getCity());
        assertEquals("US", cities.get(0).getCountryCode());
    }

    @Test
    void searchCities_keepsExactCityAndRemovesOtherCountryOrFacilityResults() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.getForEntity(
                any(URI.class),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoGeocodingResponse.class)
        )).thenReturn(ResponseEntity.ok(createMixedTokyoGeocodingResponse()));

        ResidenceCityWeatherClient client =
                new ResidenceCityWeatherClient(
                        restTemplate,
                        createProperties()
                );

        List<ResidenceCitySearchResponse> cities = client.searchCities(
                "JP",
                "Tokyo"
        );

        assertEquals(1, cities.size());
        assertEquals("Tokyo", cities.get(0).getCity());
        assertEquals("JP", cities.get(0).getCountryCode());
    }

    @Test
    void getCurrentWeather_returnsCurrentTemperatureAndCondition() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(restTemplate.getForEntity(
                any(URI.class),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoForecastResponse.class)
        )).thenReturn(ResponseEntity.ok(createForecastResponse()));

        ResidenceCityWeatherClient client =
                new ResidenceCityWeatherClient(
                        restTemplate,
                        createProperties()
                );

        ResidenceCityWeatherResponse response = client.getCurrentWeather(
                new ResidenceCitySearchResponse(
                        "Tokyo",
                        "Japan",
                        "JP",
                        "Tokyo",
                        35.6895,
                        139.6917
                )
        );

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        verify(restTemplate).getForEntity(
                uriCaptor.capture(),
                eq(ResidenceCityWeatherClient
                        .OpenMeteoForecastResponse.class)
        );

        String requestUrl = uriCaptor.getValue().toString();

        assertTrue(requestUrl.contains("latitude=35.6895"));
        assertTrue(requestUrl.contains("longitude=139.6917"));
        assertTrue(requestUrl.contains("current="));
        assertEquals("Tokyo", response.getCity());
        assertEquals(28.0, response.getTemperature());
        assertEquals(30.0, response.getFeelsLikeTemperature());
        assertEquals("비", response.getWeatherCondition());
        assertEquals(
                LocalDateTime.of(2026, 6, 24, 15, 0),
                response.getObservedAt()
        );
    }

    private OpenMeteoApiProperties createProperties() {
        OpenMeteoApiProperties properties = new OpenMeteoApiProperties();
        properties.setGeocodingBaseUrl("https://geocoding.example/v1/");
        properties.setForecastBaseUrl("https://weather.example/v1/");
        return properties;
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResponse
    createTokyoGeocodingResponse() {
        return createGeocodingResponse(List.of(
                createGeocodingResult(
                        "Tokyo",
                        "Japan",
                        "JP",
                        "Tokyo",
                        35.6895,
                        139.6917
                )
        ));
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResponse
    createNewYorkGeocodingResponse() {
        return createGeocodingResponse(List.of(
                createGeocodingResult(
                        "New York",
                        "United States",
                        "US",
                        "New York",
                        40.71427,
                        -74.00597
                )
        ));
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResponse
    createSanFranciscoGeocodingResponse() {
        return createGeocodingResponse(List.of(
                createGeocodingResult(
                        "San Francisco",
                        "United States",
                        "US",
                        "California",
                        37.77493,
                        -122.41942
                )
        ));
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResponse
    createMixedTokyoGeocodingResponse() {
        return createGeocodingResponse(List.of(
                createGeocodingResult(
                        "Tokyo Heliport",
                        "Japan",
                        "JP",
                        "Chiba",
                        35.63333,
                        139.85
                ),
                createGeocodingResult(
                        "Tokyo",
                        "Japan",
                        "JP",
                        "Tokyo",
                        35.6895,
                        139.6917
                ),
                createGeocodingResult(
                        "Tokyo",
                        "China",
                        "CN",
                        "Beijing",
                        39.9042,
                        116.4074
                )
        ));
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResponse
    createGeocodingResponse(
            List<ResidenceCityWeatherClient.OpenMeteoGeocodingResult> results
    ) {
        ResidenceCityWeatherClient.OpenMeteoGeocodingResponse response =
                new ResidenceCityWeatherClient.OpenMeteoGeocodingResponse();

        response.setResults(results);
        return response;
    }

    private ResidenceCityWeatherClient.OpenMeteoGeocodingResult
    createGeocodingResult(
            String name,
            String country,
            String countryCode,
            String admin1,
            double latitude,
            double longitude
    ) {
        ResidenceCityWeatherClient.OpenMeteoGeocodingResult result =
                new ResidenceCityWeatherClient.OpenMeteoGeocodingResult();

        result.setName(name);
        result.setCountry(country);
        result.setCountryCode(countryCode);
        result.setAdmin1(admin1);
        result.setLatitude(latitude);
        result.setLongitude(longitude);
        return result;
    }

    private ResidenceCityWeatherClient.OpenMeteoForecastResponse
    createForecastResponse() {
        ResidenceCityWeatherClient.OpenMeteoCurrentWeather current =
                new ResidenceCityWeatherClient.OpenMeteoCurrentWeather();

        current.setTime("2026-06-24T15:00");
        current.setTemperature(28.0);
        current.setApparentTemperature(30.0);
        current.setWeatherCode(61);

        ResidenceCityWeatherClient.OpenMeteoForecastResponse response =
                new ResidenceCityWeatherClient.OpenMeteoForecastResponse();

        response.setCurrent(current);
        return response;
    }
}
