package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.ResidenceCityWeatherClient;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResidenceCityWeatherServiceTest {

    @Test
    void searchCities_preservesCompactQueryForGenericClientResolution() {
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

        when(client.searchCities("US", "newyork"))
                .thenReturn(List.of(newYork));

        ResidenceCityWeatherService service =
                new ResidenceCityWeatherService(client);

        List<ResidenceCitySearchResponse> cities = service.searchCities(
                "us",
                "newyork"
        );

        assertEquals(1, cities.size());
        assertEquals("New York", cities.get(0).getCity());
        verify(client).searchCities("US", "newyork");
    }

    @Test
    void getCurrentWeather_reusesCachedResponseForSameCity() {
        ResidenceCityWeatherClient client =
                mock(ResidenceCityWeatherClient.class);

        ResidenceCitySearchResponse city = new ResidenceCitySearchResponse(
                "Tokyo",
                "Japan",
                "JP",
                "Tokyo",
                35.6895,
                139.6917
        );

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

        when(client.searchCities("JP", "Tokyo"))
                .thenReturn(List.of(city));
        when(client.getCurrentWeather(city)).thenReturn(weather);

        ResidenceCityWeatherService service =
                new ResidenceCityWeatherService(client);

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
        verify(client, times(1)).searchCities("JP", "Tokyo");
        verify(client, times(1)).getCurrentWeather(city);
    }

    @Test
    void searchCities_throwsExceptionForInvalidCountryCode() {
        ResidenceCityWeatherService service =
                new ResidenceCityWeatherService(
                        mock(ResidenceCityWeatherClient.class)
                );

        assertThrows(
                InvalidRequestException.class,
                () -> service.searchCities("Japan", "Tokyo")
        );
    }
}
