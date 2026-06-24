package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.global.config.OpenMeteoApiProperties;
import com.example.Chungbuk.global.exception.ResidenceWeatherApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Component
public class ResidenceCityWeatherClient {

    private static final String CURRENT_WEATHER_FIELDS =
            "temperature_2m,apparent_temperature,weather_code";

    private final RestTemplate openMeteoRestTemplate;
    private final OpenMeteoApiProperties openMeteoApiProperties;

    public ResidenceCityWeatherClient(
            @Qualifier("openMeteoRestTemplate")
            RestTemplate openMeteoRestTemplate,
            OpenMeteoApiProperties openMeteoApiProperties
    ) {
        this.openMeteoRestTemplate = openMeteoRestTemplate;
        this.openMeteoApiProperties = openMeteoApiProperties;
    }

    public List<ResidenceCitySearchResponse> searchCities(
            String countryCode,
            String query
    ) {
        openMeteoApiProperties.validateUrls();

        URI requestUri = UriComponentsBuilder
                .fromUriString(openMeteoApiProperties.getGeocodingSearchUrl())
                .queryParam("name", query)
                .queryParam("count", 10)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .queryParam("countryCode", countryCode)
                .build()
                .encode()
                .toUri();

        try {
            ResponseEntity<OpenMeteoGeocodingResponse> responseEntity =
                    openMeteoRestTemplate.getForEntity(
                            requestUri,
                            OpenMeteoGeocodingResponse.class
                    );

            OpenMeteoGeocodingResponse body = responseEntity.getBody();
            if (body == null || body.getResults() == null) {
                return Collections.emptyList();
            }

            return body.getResults().stream()
                    .map(this::toResidenceCity)
                    .toList();
        } catch (RestClientException exception) {
            throw new ResidenceWeatherApiException(
                    "현재 거주 도시를 검색하지 못했습니다.",
                    exception
            );
        }
    }

    public ResidenceCityWeatherResponse getCurrentWeather(
            ResidenceCitySearchResponse city
    ) {
        openMeteoApiProperties.validateUrls();

        URI requestUri = UriComponentsBuilder
                .fromUriString(openMeteoApiProperties.getForecastUrl())
                .queryParam("latitude", city.getLatitude())
                .queryParam("longitude", city.getLongitude())
                .queryParam("current", CURRENT_WEATHER_FIELDS)
                .queryParam("timezone", "auto")
                .build()
                .encode()
                .toUri();

        try {
            ResponseEntity<OpenMeteoForecastResponse> responseEntity =
                    openMeteoRestTemplate.getForEntity(
                            requestUri,
                            OpenMeteoForecastResponse.class
                    );

            OpenMeteoForecastResponse body = responseEntity.getBody();
            validateCurrentWeather(body);

            OpenMeteoCurrentWeather current = body.getCurrent();

            return new ResidenceCityWeatherResponse(
                    city.getCity(),
                    city.getCountry(),
                    city.getCountryCode(),
                    city.getAdmin1(),
                    city.getLatitude(),
                    city.getLongitude(),
                    parseObservedAt(current.getTime()),
                    current.getTemperature(),
                    current.getApparentTemperature(),
                    toWeatherCondition(current.getWeatherCode())
            );
        } catch (RestClientException exception) {
            throw new ResidenceWeatherApiException(
                    "현재 거주 도시 날씨를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private ResidenceCitySearchResponse toResidenceCity(
            OpenMeteoGeocodingResult result
    ) {
        return new ResidenceCitySearchResponse(
                result.getName(),
                result.getCountry(),
                result.getCountryCode(),
                result.getAdmin1(),
                result.getLatitude(),
                result.getLongitude()
        );
    }

    private void validateCurrentWeather(
            OpenMeteoForecastResponse response
    ) {
        if (response == null
                || response.getCurrent() == null
                || response.getCurrent().getTime() == null
                || response.getCurrent().getTemperature() == null
                || response.getCurrent().getApparentTemperature() == null
                || response.getCurrent().getWeatherCode() == null) {

            throw new ResidenceWeatherApiException(
                    "현재 거주 도시 날씨 응답 형식이 올바르지 않습니다."
            );
        }
    }

    private LocalDateTime parseObservedAt(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResidenceWeatherApiException(
                    "현재 거주 도시 날씨 시각 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private String toWeatherCondition(int weatherCode) {
        if (weatherCode == 0) {
            return "맑음";
        }

        if (weatherCode == 1 || weatherCode == 2) {
            return "대체로 맑음";
        }

        if (weatherCode == 3) {
            return "흐림";
        }

        if (weatherCode == 45 || weatherCode == 48) {
            return "안개";
        }

        if (weatherCode >= 51 && weatherCode <= 57) {
            return "이슬비";
        }

        if ((weatherCode >= 61 && weatherCode <= 67)
                || (weatherCode >= 80 && weatherCode <= 82)) {
            return "비";
        }

        if ((weatherCode >= 71 && weatherCode <= 77)
                || (weatherCode >= 85 && weatherCode <= 86)) {
            return "눈";
        }

        if (weatherCode >= 95) {
            return "뇌우";
        }

        return "알 수 없음";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoGeocodingResponse {

        private List<OpenMeteoGeocodingResult> results;

        public List<OpenMeteoGeocodingResult> getResults() {
            return results;
        }

        public void setResults(
                List<OpenMeteoGeocodingResult> results
        ) {
            this.results = results;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoGeocodingResult {

        private String name;
        private String country;
        private String admin1;
        private double latitude;
        private double longitude;

        @JsonProperty("country_code")
        private String countryCode;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getAdmin1() {
            return admin1;
        }

        public void setAdmin1(String admin1) {
            this.admin1 = admin1;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoForecastResponse {

        private OpenMeteoCurrentWeather current;

        public OpenMeteoCurrentWeather getCurrent() {
            return current;
        }

        public void setCurrent(OpenMeteoCurrentWeather current) {
            this.current = current;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoCurrentWeather {

        private String time;

        @JsonProperty("temperature_2m")
        private Double temperature;

        @JsonProperty("apparent_temperature")
        private Double apparentTemperature;

        @JsonProperty("weather_code")
        private Integer weatherCode;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getApparentTemperature() {
            return apparentTemperature;
        }

        public void setApparentTemperature(
                Double apparentTemperature
        ) {
            this.apparentTemperature = apparentTemperature;
        }

        public Integer getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(Integer weatherCode) {
            this.weatherCode = weatherCode;
        }
    }
}
