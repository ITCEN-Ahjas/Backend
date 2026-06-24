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
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeatherServiceTest {

    @Test
    void getRegionTimeSlotWeather_returnsRemainingTimeSlotResponses() {
        WeatherRegionService weatherRegionService =
                mock(WeatherRegionService.class);
        KmaWeatherClient kmaWeatherClient =
                mock(KmaWeatherClient.class);
        WeatherDataNormalizeService weatherDataNormalizeService =
                mock(WeatherDataNormalizeService.class);
        FeelsLikeWeatherService feelsLikeWeatherService =
                mock(FeelsLikeWeatherService.class);

        WeatherService weatherService = new WeatherService(
                weatherRegionService,
                kmaWeatherClient,
                weatherDataNormalizeService,
                feelsLikeWeatherService
        );

        RegionWeatherRequest request = new RegionWeatherRequest();
        request.setRegion("청주");

        List<KmaWeatherItem> villageForecastItems = List.of(
                baseWeatherItem("20260621", "0800")
        );

        Map<WeatherTimeSlot, ForecastWeatherSnapshot> forecasts =
                createRemainingTimeSlotForecasts();

        when(weatherRegionService.getRegion("청주"))
                .thenReturn(ChungbukRegion.CHEONGJU);

        when(kmaWeatherClient.getVilageFcst(
                ChungbukRegion.CHEONGJU
        )).thenReturn(villageForecastItems);

        when(weatherDataNormalizeService.normalizeTimeSlotForecasts(
                eq(ChungbukRegion.CHEONGJU),
                eq(villageForecastItems),
                any(LocalDateTime.class)
        )).thenReturn(forecasts);

        when(feelsLikeWeatherService.create(any(
                CurrentWeatherResponse.class
        ))).thenReturn(createFeelsLikeWeather());

        RegionTimeSlotWeatherResponse response =
                weatherService.getRegionTimeSlotWeather(request);

        assertEquals("청주", response.getRegion());
        assertEquals(
                LocalDateTime.of(2026, 6, 21, 8, 0),
                response.getUpdatedAt()
        );
        assertEquals(
                LocalDate.of(2026, 6, 21),
                response.getForecastDate()
        );
        assertEquals(2, response.getTimeSlots().size());
        assertEquals(
                "afternoon",
                response.getTimeSlots().get(0).getTimeSlot()
        );
        assertEquals(
                "저녁",
                response.getTimeSlots().get(1).getTimeSlotName()
        );
        assertEquals(
                26.0,
                response.getTimeSlots()
                        .get(0)
                        .getCurrentWeather()
                        .getTemperature()
        );

        verify(kmaWeatherClient).getVilageFcst(
                ChungbukRegion.CHEONGJU
        );
    }

    private Map<WeatherTimeSlot, ForecastWeatherSnapshot>
    createRemainingTimeSlotForecasts() {
        Map<WeatherTimeSlot, ForecastWeatherSnapshot> forecasts =
                new EnumMap<>(WeatherTimeSlot.class);

        forecasts.put(
                WeatherTimeSlot.AFTERNOON,
                snapshot(15, 26.0)
        );
        forecasts.put(
                WeatherTimeSlot.EVENING,
                snapshot(19, 20.0)
        );

        return forecasts;
    }

    private ForecastWeatherSnapshot snapshot(
            int hour,
            double temperature
    ) {
        return new ForecastWeatherSnapshot(
                LocalDateTime.of(2026, 6, 21, hour, 0),
                new CurrentWeatherResponse(
                        "청주",
                        temperature,
                        60,
                        2.0,
                        "보통",
                        "강수 없음",
                        "강수 없음",
                        10,
                        "맑음",
                        "맑음"
                )
        );
    }

    private FeelsLikeWeatherResponse createFeelsLikeWeather() {
        return new FeelsLikeWeatherResponse(
                18.0,
                0.0,
                "현재 기온과 비슷하게 느껴집니다.",
                "현재 기온과 체감온도가 비슷합니다.",
                "현재 기온과 체감온도 차이가 크지 않습니다.",
                List.of("현재 기온")
        );
    }

    private KmaWeatherItem baseWeatherItem(
            String baseDate,
            String baseTime
    ) {
        KmaWeatherItem item = new KmaWeatherItem();
        item.setBaseDate(baseDate);
        item.setBaseTime(baseTime);
        item.setCategory("TMP");
        item.setFcstDate("20260621");
        item.setFcstTime("1500");
        item.setFcstValue("26");

        return item;
    }
}
