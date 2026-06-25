package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidenceCityIndexTest {

    private final ResidenceCityIndex residenceCityIndex =
            new ResidenceCityIndex(new ObjectMapper());

    @Test
    void search_returnsMajorCitiesForTwoCharacterPrefix() {
        List<ResidenceCitySearchResponse> cities = residenceCityIndex.search(
                "US",
                "ne"
        );

        assertTrue(cities.stream()
                .anyMatch(city -> city.getCity().equals("New York")));
        assertTrue(cities.stream()
                .anyMatch(city -> city.getCity().equals("New Orleans")));
        assertEquals("New York", cities.get(0).getCity());
    }

    @Test
    void findExactCity_resolvesSpacelessCityName() {
        ResidenceCitySearchResponse city = residenceCityIndex.findExactCity(
                        "US",
                        "newyork"
                )
                .orElseThrow();

        assertEquals("New York", city.getCity());
        assertEquals("US", city.getCountryCode());
    }
}
