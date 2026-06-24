package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.KmaWeatherClient;
import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ForecastWeatherSnapshot;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherRegionsResponse;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

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

    public RegionTimeSlotWeatherResponse getRegionTimeSlotWeather(
            RegionWeatherRequest request
    ) {
        ChungbukRegion region = resolveRegion(request.getRegion());

        List<KmaWeatherItem> villageForecastItems =
                kmaWeatherClient.getVilageFcst(region);

        Map<WeatherTimeSlot, ForecastWeatherSnapshot>
                timeSlotForecasts =
                weatherDataNormalizeService
                        .normalizeTimeSlotForecasts(
                                region,
                                villageForecastItems,
                                LocalDateTime.now(KOREA_ZONE_ID)
                        );

        List<TimeSlotWeatherResponse> timeSlots = new ArrayList<>();

        for (WeatherTimeSlot timeSlot : WeatherTimeSlot.values()) {
            ForecastWeatherSnapshot snapshot =
                    timeSlotForecasts.get(timeSlot);

            if (snapshot == null) {
                continue;
            }

            FeelsLikeWeatherResponse feelsLikeWeather =
                    feelsLikeWeatherService.create(
                            snapshot.getCurrentWeather()
                    );

            timeSlots.add(
                    new TimeSlotWeatherResponse(
                            timeSlot,
                            snapshot.getForecastAt(),
                            snapshot.getCurrentWeather(),
                            feelsLikeWeather
                    )
            );
        }

        if (timeSlots.isEmpty()) {
            throw new KmaWeatherApiException(
                    "표시할 시간대별 날씨 데이터가 없습니다."
            );
        }

        LocalDate forecastDate = timeSlots.get(0)
                .getForecastAt()
                .toLocalDate();

        return new RegionTimeSlotWeatherResponse(
                region.getDisplayName(),
                extractUpdatedAt(villageForecastItems),
                forecastDate,
                timeSlots
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
            List<KmaWeatherItem> weatherItems
    ) {
        KmaWeatherItem representativeItem = weatherItems.stream()
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
