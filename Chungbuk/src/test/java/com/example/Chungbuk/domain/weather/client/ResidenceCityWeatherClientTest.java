package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.config.OpenMeteoApiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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
        )).thenReturn(ResponseEntity.ok(createGeocodingResponse()));

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
    createGeocodingResponse() {
        ResidenceCityWeatherClient.OpenMeteoGeocodingResult result =
                new ResidenceCityWeatherClient.OpenMeteoGeocodingResult();

        result.setName("Tokyo");
        result.setCountry("Japan");
        result.setCountryCode("JP");
        result.setAdmin1("Tokyo");
        result.setLatitude(35.6895);
        result.setLongitude(139.6917);

        ResidenceCityWeatherClient.OpenMeteoGeocodingResponse response =
                new ResidenceCityWeatherClient.OpenMeteoGeocodingResponse();

        response.setResults(List.of(result));
        return response;
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
