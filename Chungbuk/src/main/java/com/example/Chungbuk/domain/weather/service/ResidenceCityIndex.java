package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ResidenceCityIndex {

    private static final String CITY_INDEX_RESOURCE =
            "cities/world-major-cities.json";
    private static final int MAX_CITY_RESULTS = 10;

    private final List<IndexedCity> cities;

    public ResidenceCityIndex() {
        this(new ObjectMapper());
    }

    ResidenceCityIndex(ObjectMapper objectMapper) {
        this.cities = loadCities(objectMapper);
    }

    public List<ResidenceCitySearchResponse> search(
            String countryCode,
            String query
    ) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedQuery = normalizeCityKey(query);

        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return cities.stream()
                .filter(city -> city.countryCode()
                        .equals(normalizedCountryCode))
                .filter(city -> normalizeCityKey(city.city())
                        .contains(normalizedQuery))
                .sorted(Comparator
                        .comparingInt((IndexedCity city) ->
                                calculateMatchRank(city.city(), normalizedQuery))
                        .thenComparingInt(IndexedCity::priority)
                        .thenComparing(IndexedCity::city))
                .limit(MAX_CITY_RESULTS)
                .map(IndexedCity::toResponse)
                .toList();
    }

    public Optional<ResidenceCitySearchResponse> findExactCity(
            String countryCode,
            String cityName
    ) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedCityName = normalizeCityKey(cityName);

        if (normalizedCityName.isBlank()) {
            return Optional.empty();
        }

        return cities.stream()
                .filter(city -> city.countryCode()
                        .equals(normalizedCountryCode))
                .filter(city -> normalizeCityKey(city.city())
                        .equals(normalizedCityName))
                .min(Comparator.comparingInt(IndexedCity::priority))
                .map(IndexedCity::toResponse);
    }

    private List<IndexedCity> loadCities(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(
                CITY_INDEX_RESOURCE
        );

        try (InputStream inputStream = resource.getInputStream()) {
            List<IndexedCity> loadedCities = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );

            if (loadedCities == null || loadedCities.isEmpty()) {
                throw new IllegalStateException(
                        "세계 주요 도시 인덱스가 비어 있습니다."
                );
            }

            return List.copyOf(loadedCities);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "세계 주요 도시 인덱스를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private int calculateMatchRank(
            String cityName,
            String normalizedQuery
    ) {
        String normalizedCityName = normalizeCityKey(cityName);

        if (normalizedCityName.equals(normalizedQuery)) {
            return 0;
        }

        if (normalizedCityName.startsWith(normalizedQuery)) {
            return 1;
        }

        return 2;
    }

    private String normalizeCountryCode(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCityKey(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "");
    }

    public record IndexedCity(
            String city,
            String country,
            String countryCode,
            String admin1,
            double latitude,
            double longitude,
            int priority
    ) {

        ResidenceCitySearchResponse toResponse() {
            return new ResidenceCitySearchResponse(
                    city,
                    country,
                    countryCode,
                    admin1,
                    latitude,
                    longitude
            );
        }
    }
}
