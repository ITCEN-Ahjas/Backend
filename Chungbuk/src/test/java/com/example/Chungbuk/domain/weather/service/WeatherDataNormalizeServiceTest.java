package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherDataNormalizeServiceTest {

    private final WeatherDataNormalizeService weatherDataNormalizeService =
            new WeatherDataNormalizeService();

    @Test
    @DisplayName("비가 있으면 하늘 상태보다 비 상태를 우선 반환한다")
    void normalize_prioritizesRainOverSkyStatus() {
        CurrentWeatherResponse response =
                weatherDataNormalizeService.normalize(
                        ChungbukRegion.CHEONGJU,
                        List.of(
                                nowcastItem("T1H", "22.4"),
                                nowcastItem("REH", "63"),
                                nowcastItem("WSD", "2.4"),
                                nowcastItem("RN1", "1.0"),
                                nowcastItem("PTY", "1")
                        ),
                        List.of(
                                forecastItem("T1H", "22.0"),
                                forecastItem("SKY", "1"),
                                forecastItem("PTY", "1"),
                                forecastItem("POP", "70")
                        )
                );

        assertEquals("청주", response.getRegion());
        assertEquals(22.4, response.getTemperature());
        assertEquals(63, response.getHumidity());
        assertEquals(2.4, response.getWindSpeed());
        assertEquals("보통", response.getWindStatus());
        assertEquals("1.0", response.getPrecipitationAmount());
        assertEquals("비", response.getPrecipitationType());
        assertEquals(70, response.getPrecipitationProbability());
        assertEquals("맑음", response.getSkyStatus());
        assertEquals("비", response.getWeatherCondition());
    }

    @Test
    @DisplayName("강수가 없으면 하늘 상태를 대표 날씨로 반환한다")
    void normalize_usesSkyStatusWhenThereIsNoPrecipitation() {
        CurrentWeatherResponse response =
                weatherDataNormalizeService.normalize(
                        ChungbukRegion.DANYANG,
                        List.of(
                                nowcastItem("T1H", "18.0"),
                                nowcastItem("REH", "55"),
                                nowcastItem("WSD", "0.8"),
                                nowcastItem("RN1", "강수없음"),
                                nowcastItem("PTY", "0")
                        ),
                        List.of(
                                forecastItem("T1H", "18.0"),
                                forecastItem("SKY", "3"),
                                forecastItem("PTY", "0"),
                                forecastItem("POP", "20")
                        )
                );

        assertEquals("강수 없음", response.getPrecipitationType());
        assertEquals("구름 많음", response.getSkyStatus());
        assertEquals("구름 많음", response.getWeatherCondition());
        assertEquals("약함", response.getWindStatus());
    }

    private KmaWeatherItem nowcastItem(
            String category,
            String observationValue
    ) {
        KmaWeatherItem item = new KmaWeatherItem();

        item.setCategory(category);
        item.setObsrValue(observationValue);

        return item;
    }

    private KmaWeatherItem forecastItem(
            String category,
            String forecastValue
    ) {
        KmaWeatherItem item = new KmaWeatherItem();

        item.setCategory(category);
        item.setFcstDate("20260621");
        item.setFcstTime("1300");
        item.setFcstValue(forecastValue);

        return item;
    }
}