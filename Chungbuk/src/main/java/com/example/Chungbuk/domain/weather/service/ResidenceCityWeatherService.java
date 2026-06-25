package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.ResidenceCityWeatherClient;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import com.example.Chungbuk.global.exception.ResidenceWeatherApiException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
public class ResidenceCityWeatherService {

    private static final int MIN_CITY_QUERY_LENGTH = 2;
    private static final int SHORT_CITY_QUERY_LENGTH = 2;
    private static final int MAX_CITY_SEARCH_RESULTS = 10;

    private final ResidenceCityWeatherClient residenceCityWeatherClient;
    private final ResidenceCityIndex residenceCityIndex;

    private final Cache<String, ResidenceCityWeatherResponse>
            weatherCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(45))
            .maximumSize(500)
            .build();

    public ResidenceCityWeatherService(
            ResidenceCityWeatherClient residenceCityWeatherClient,
            ResidenceCityIndex residenceCityIndex
    ) {
        this.residenceCityWeatherClient = residenceCityWeatherClient;
        this.residenceCityIndex = residenceCityIndex;
    }

    public List<ResidenceCitySearchResponse> searchCities(
            String countryCode,
            String query
    ) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedQuery = normalizeCityQuery(query);
        List<ResidenceCitySearchResponse> indexedCities =
                residenceCityIndex.search(
                        normalizedCountryCode,
                        normalizedQuery
                );

        if (createCityKey(normalizedQuery).length()
                <= SHORT_CITY_QUERY_LENGTH) {
            return indexedCities;
        }

        try {
            List<ResidenceCitySearchResponse> apiCities =
                    residenceCityWeatherClient.searchCities(
                            normalizedCountryCode,
                            normalizedQuery
                    );

            return mergeAndRankCities(
                    indexedCities,
                    apiCities,
                    normalizedQuery
            );
        } catch (ResidenceWeatherApiException exception) {
            if (!indexedCities.isEmpty()) {
                return indexedCities;
            }

            throw exception;
        }
    }

    public ResidenceCityWeatherResponse getCurrentWeather(
            String countryCode,
            String city
    ) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedCity = normalizeCityQuery(city);
        String cacheKey = createCacheKey(
                normalizedCountryCode,
                normalizedCity
        );

        try {
            return weatherCache.get(
                    cacheKey,
                    ignored -> requestCurrentWeather(
                            normalizedCountryCode,
                            normalizedCity
                    )
            );
        } catch (ResidenceWeatherApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResidenceWeatherApiException(
                    "현재 거주 도시 날씨를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private ResidenceCityWeatherResponse requestCurrentWeather(
            String countryCode,
            String city
    ) {
        ResidenceCitySearchResponse selectedCity = residenceCityIndex
                .findExactCity(countryCode, city)
                .orElseGet(() -> findCityFromApi(countryCode, city));

        return residenceCityWeatherClient.getCurrentWeather(selectedCity);
    }

    private ResidenceCitySearchResponse findCityFromApi(
            String countryCode,
            String city
    ) {
        return residenceCityWeatherClient.searchCities(countryCode, city)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResidenceWeatherApiException(
                        "입력한 현재 거주 도시를 찾을 수 없습니다."
                ));
    }

    private List<ResidenceCitySearchResponse> mergeAndRankCities(
            List<ResidenceCitySearchResponse> indexedCities,
            List<ResidenceCitySearchResponse> apiCities,
            String query
    ) {
        Map<String, ResidenceCitySearchResponse> uniqueCities =
                new LinkedHashMap<>();

        addCities(uniqueCities, indexedCities);
        addCities(uniqueCities, apiCities);

        return uniqueCities.values()
                .stream()
                .sorted((first, second) -> Integer.compare(
                        calculateMatchRank(first.getCity(), query),
                        calculateMatchRank(second.getCity(), query)
                ))
                .limit(MAX_CITY_SEARCH_RESULTS)
                .toList();
    }

    private void addCities(
            Map<String, ResidenceCitySearchResponse> uniqueCities,
            List<ResidenceCitySearchResponse> cities
    ) {
        if (cities == null) {
            return;
        }

        cities.forEach(city -> uniqueCities.putIfAbsent(
                createCityKey(city.getCity())
                        + ":"
                        + city.getCountryCode().toUpperCase(Locale.ROOT),
                city
        ));
    }

    private int calculateMatchRank(String cityName, String query) {
        String cityKey = createCityKey(cityName);
        String queryKey = createCityKey(query);

        if (cityKey.equals(queryKey)) {
            return 0;
        }

        if (cityKey.startsWith(queryKey)) {
            return 1;
        }

        return 2;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || !countryCode.trim().matches("[A-Za-z]{2}")) {
            throw new InvalidRequestException(
                    "국가 코드는 ISO 2자리 코드로 입력해 주세요."
            );
        }

        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCityQuery(String city) {
        if (city == null) {
            throw new InvalidRequestException(
                    "현재 거주 도시는 두 글자 이상 입력해 주세요."
            );
        }

        String normalizedSpacing = city.trim()
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        String cityKey = createCityKey(normalizedSpacing);

        if (cityKey.length() < MIN_CITY_QUERY_LENGTH) {
            throw new InvalidRequestException(
                    "현재 거주 도시는 두 글자 이상 입력해 주세요."
            );
        }

        return normalizedSpacing;
    }

    private String createCacheKey(
            String countryCode,
            String city
    ) {
        return countryCode + ":" + createCityKey(city);
    }

    private String createCityKey(String city) {
        return city.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "");
    }
}
