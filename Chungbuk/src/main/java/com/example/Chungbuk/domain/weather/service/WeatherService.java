package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.KmaWeatherClient;
import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherRegionsResponse;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class WeatherService {

    private static final DateTimeFormatter KMA_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final WeatherRegionService weatherRegionService;
    private final KmaWeatherClient kmaWeatherClient;
    private final WeatherDataNormalizeService weatherDataNormalizeService;
    private final FeelsLikeWeatherService feelsLikeWeatherService;

    public WeatherService(
            WeatherRegionService weatherRegionService,
            KmaWeatherClient kmaWeatherClient,
            WeatherDataNormalizeService weatherDataNormalizeService,
            FeelsLikeWeatherService feelsLikeWeatherService
    ) {
        this.weatherRegionService = weatherRegionService;
        this.kmaWeatherClient = kmaWeatherClient;
        this.weatherDataNormalizeService = weatherDataNormalizeService;
        this.feelsLikeWeatherService = feelsLikeWeatherService;
    }

    public WeatherRegionsResponse getSupportedRegions() {
        return new WeatherRegionsResponse(
                weatherRegionService.getAllRegionNames()
        );
    }

    public WeatherPageResponse getRegionWeather(
            RegionWeatherRequest request
    ) {
        ChungbukRegion region = resolveRegion(request.getRegion());

        List<KmaWeatherItem> nowcastItems =
                kmaWeatherClient.getUltraSrtNcst(region);

        List<KmaWeatherItem> forecastItems =
                kmaWeatherClient.getUltraSrtFcst(region);

        CurrentWeatherResponse currentWeather =
                weatherDataNormalizeService.normalize(
                        region,
                        nowcastItems,
                        forecastItems
                );

        FeelsLikeWeatherResponse feelsLikeWeather =
                feelsLikeWeatherService.create(currentWeather);

        return new WeatherPageResponse(
                region.getDisplayName(),
                extractUpdatedAt(nowcastItems),
                currentWeather,
                feelsLikeWeather
        );
    }

    private ChungbukRegion resolveRegion(String regionName) {
        try {
            return weatherRegionService.getRegion(regionName);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(exception.getMessage());
        }
    }

    private LocalDateTime extractUpdatedAt(
            List<KmaWeatherItem> nowcastItems
    ) {
        KmaWeatherItem representativeItem = nowcastItems.stream()
                .filter(item -> hasText(item.getBaseDate()))
                .filter(item -> hasText(item.getBaseTime()))
                .findFirst()
                .orElseThrow(() -> new KmaWeatherApiException(
                        "기상청 날씨 갱신 시각을 확인할 수 없습니다."
                ));

        try {
            return LocalDateTime.parse(
                    representativeItem.getBaseDate()
                            + representativeItem.getBaseTime(),
                    KMA_DATE_TIME_FORMATTER
            );
        } catch (DateTimeParseException exception) {
            throw new KmaWeatherApiException(
                    "기상청 날씨 갱신 시각 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}