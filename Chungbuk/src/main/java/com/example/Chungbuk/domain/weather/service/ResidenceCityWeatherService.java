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
import java.util.List;
import java.util.Locale;

@Service
public class ResidenceCityWeatherService {

    private static final int MIN_CITY_QUERY_LENGTH = 2;

    private final ResidenceCityWeatherClient residenceCityWeatherClient;

    private final Cache<String, ResidenceCityWeatherResponse>
            weatherCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(45))
            .maximumSize(500)
            .build();

    public ResidenceCityWeatherService(
            ResidenceCityWeatherClient residenceCityWeatherClient
    ) {
        this.residenceCityWeatherClient = residenceCityWeatherClient;
    }

    public List<ResidenceCitySearchResponse> searchCities(
            String countryCode,
            String query
    ) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedQuery = normalizeCityQuery(query);

        return residenceCityWeatherClient.searchCities(
                normalizedCountryCode,
                normalizedQuery
        );
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
        List<ResidenceCitySearchResponse> cities =
                residenceCityWeatherClient.searchCities(
                        countryCode,
                        city
                );

        ResidenceCitySearchResponse selectedCity = cities.stream()
                .findFirst()
                .orElseThrow(() -> new ResidenceWeatherApiException(
                        "입력한 현재 거주 도시를 찾을 수 없습니다."
                ));

        return residenceCityWeatherClient.getCurrentWeather(selectedCity);
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
        if (city == null || city.trim().length() < MIN_CITY_QUERY_LENGTH) {
            throw new InvalidRequestException(
                    "현재 거주 도시는 두 글자 이상 입력해 주세요."
            );
        }

        return city.trim();
    }

    private String createCacheKey(
            String countryCode,
            String city
    ) {
        return countryCode + ":" + city.toLowerCase(Locale.ROOT);
    }
}
