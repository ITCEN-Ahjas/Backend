package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.ResidenceCityWeatherClient;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResidenceCityWeatherServiceTest {

    @Test
    void searchCities_returnsIndexedMajorCitiesForTwoCharacterQuery() {
        ResidenceCityWeatherClient client =
                mock(ResidenceCityWeatherClient.class);
        ResidenceCityWeatherService service = createService(client);

        List<ResidenceCitySearchResponse> cities = service.searchCities(
                "us",
                "ne"
        );

        assertTrue(cities.stream()
                .anyMatch(city -> city.getCity().equals("New York")));
        assertTrue(cities.stream()
                .anyMatch(city -> city.getCity().equals("New Orleans")));
        verify(client, never()).searchCities(any(), any());
    }

    @Test
    void searchCities_mergesIndexedAndApiResultsForLongQuery() {
        ResidenceCityWeatherClient client =
                mock(ResidenceCityWeatherClient.class);
        ResidenceCitySearchResponse newYork =
                new ResidenceCitySearchResponse(
                        "New York",
                        "United States",
                        "US",
                        "New York",
                        40.71427,
                        -74.00597
                );
        ResidenceCitySearchResponse newburgh =
                new ResidenceCitySearchResponse(
                        "Newburgh",
                        "United States",
                        "US",
                        "New York",
                        41.50343,
                        -74.01042
                );

        when(client.searchCities("US", "New York"))
                .thenReturn(List.of(newYork, newburgh));

        ResidenceCityWeatherService service = createService(client);
        List<ResidenceCitySearchResponse> cities = service.searchCities(
                "us",
                "New York"
        );

        assertEquals("New York", cities.get(0).getCity());
        assertTrue(cities.stream()
                .anyMatch(city -> city.getCity().equals("Newburgh")));
        verify(client).searchCities("US", "New York");
    }

    @Test
    void getCurrentWeather_reusesCachedResponseForIndexedCity() {
        ResidenceCityWeatherClient client =
                mock(ResidenceCityWeatherClient.class);
        ResidenceCityWeatherResponse weather =
                new ResidenceCityWeatherResponse(
                        "Tokyo",
                        "Japan",
                        "JP",
                        "Tokyo",
                        35.6895,
                        139.6917,
                        LocalDateTime.of(2026, 6, 24, 15, 0),
                        28.0,
                        30.0,
                        "비"
                );

        when(client.getCurrentWeather(any())).thenReturn(weather);

        ResidenceCityWeatherService service = createService(client);
        ResidenceCityWeatherResponse first = service.getCurrentWeather(
                "jp",
                "Tokyo"
        );
        ResidenceCityWeatherResponse second = service.getCurrentWeather(
                "JP",
                "Tokyo"
        );

        assertEquals(30.0, first.getFeelsLikeTemperature());
        assertEquals(30.0, second.getFeelsLikeTemperature());
        verify(client, times(1)).getCurrentWeather(any());
        verify(client, never()).searchCities(any(), any());
    }

    @Test
    void searchCities_throwsExceptionForInvalidCountryCode() {
        ResidenceCityWeatherService service = createService(
                mock(ResidenceCityWeatherClient.class)
        );

        assertThrows(
                InvalidRequestException.class,
                () -> service.searchCities("Japan", "Tokyo")
        );
    }

    private ResidenceCityWeatherService createService(
            ResidenceCityWeatherClient client
    ) {
        return new ResidenceCityWeatherService(
                client,
                new ResidenceCityIndex(new ObjectMapper())
        );
    }
}
