package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ForecastWeatherSnapshot;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Test
    @DisplayName("하루가 시작되기 전에는 아침부터 저녁까지 네 시간대 예보를 반환한다")
    void normalizeTimeSlotForecasts_returnsFourTimeSlotForecastsBeforeMorning() {
        Map<WeatherTimeSlot, ForecastWeatherSnapshot> responses =
                weatherDataNormalizeService.normalizeTimeSlotForecasts(
                        ChungbukRegion.CHEONGJU,
                        createOneDayForecastItems("20260621"),
                        LocalDateTime.of(2026, 6, 21, 7, 30)
                );

        assertEquals(4, responses.size());
        assertEquals(
                LocalDateTime.of(2026, 6, 21, 9, 0),
                responses.get(WeatherTimeSlot.MORNING).getForecastAt()
        );
        assertEquals(
                24.0,
                responses.get(WeatherTimeSlot.DAYTIME)
                        .getCurrentWeather()
                        .getTemperature()
        );
        assertEquals(
                "비",
                responses.get(WeatherTimeSlot.AFTERNOON)
                        .getCurrentWeather()
                        .getWeatherCondition()
        );
        assertEquals(
                20.0,
                responses.get(WeatherTimeSlot.EVENING)
                        .getCurrentWeather()
                        .getTemperature()
        );
    }

    @Test
    @DisplayName("오후에는 오늘 남은 오후와 저녁 시간대 예보만 반환한다")
    void normalizeTimeSlotForecasts_returnsTodayRemainingTimeSlots() {
        Map<WeatherTimeSlot, ForecastWeatherSnapshot> responses =
                weatherDataNormalizeService.normalizeTimeSlotForecasts(
                        ChungbukRegion.CHEONGJU,
                        List.of(
                                villageForecastItem(
                                        "20260621", "1500", "TMP", "26"
                                ),
                                villageForecastItem(
                                        "20260621", "1900", "TMP", "23"
                                ),
                                villageForecastItem(
                                        "20260622", "0900", "TMP", "21"
                                ),
                                villageForecastItem(
                                        "20260622", "1200", "TMP", "25"
                                ),
                                villageForecastItem(
                                        "20260622", "1500", "TMP", "27"
                                ),
                                villageForecastItem(
                                        "20260622", "1900", "TMP", "22"
                                )
                        ),
                        LocalDateTime.of(2026, 6, 21, 14, 0)
                );

        assertEquals(2, responses.size());
        assertEquals(
                LocalDateTime.of(2026, 6, 21, 15, 0),
                responses.get(WeatherTimeSlot.AFTERNOON).getForecastAt()
        );
        assertEquals(
                LocalDateTime.of(2026, 6, 21, 19, 0),
                responses.get(WeatherTimeSlot.EVENING).getForecastAt()
        );
    }

    @Test
    @DisplayName("저녁 시간이 끝난 뒤에는 다음 날짜의 네 시간대 예보를 반환한다")
    void normalizeTimeSlotForecasts_returnsNextDayAfterEvening() {
        Map<WeatherTimeSlot, ForecastWeatherSnapshot> responses =
                weatherDataNormalizeService.normalizeTimeSlotForecasts(
                        ChungbukRegion.CHEONGJU,
                        List.of(
                                villageForecastItem(
                                        "20260621", "1900", "TMP", "23"
                                ),
                                villageForecastItem(
                                        "20260622", "0900", "TMP", "21"
                                ),
                                villageForecastItem(
                                        "20260622", "1200", "TMP", "25"
                                ),
                                villageForecastItem(
                                        "20260622", "1500", "TMP", "27"
                                ),
                                villageForecastItem(
                                        "20260622", "1900", "TMP", "22"
                                )
                        ),
                        LocalDateTime.of(2026, 6, 21, 21, 0)
                );

        assertEquals(4, responses.size());

        for (ForecastWeatherSnapshot response : responses.values()) {
            assertEquals(
                    LocalDate.of(2026, 6, 22),
                    response.getForecastAt().toLocalDate()
            );
        }
    }

    private List<KmaWeatherItem> createOneDayForecastItems(
            String forecastDate
    ) {
        return List.of(
                villageForecastItem(forecastDate, "0900", "TMP", "18"),
                villageForecastItem(forecastDate, "0900", "SKY", "1"),
                villageForecastItem(forecastDate, "0900", "PTY", "0"),
                villageForecastItem(forecastDate, "0900", "POP", "0"),
                villageForecastItem(forecastDate, "1200", "TMP", "24"),
                villageForecastItem(forecastDate, "1200", "SKY", "3"),
                villageForecastItem(forecastDate, "1200", "PTY", "0"),
                villageForecastItem(forecastDate, "1200", "POP", "20"),
                villageForecastItem(forecastDate, "1500", "TMP", "26"),
                villageForecastItem(forecastDate, "1500", "SKY", "4"),
                villageForecastItem(forecastDate, "1500", "PTY", "1"),
                villageForecastItem(forecastDate, "1500", "POP", "70"),
                villageForecastItem(forecastDate, "1900", "TMP", "20"),
                villageForecastItem(forecastDate, "1900", "SKY", "3"),
                villageForecastItem(forecastDate, "1900", "PTY", "0"),
                villageForecastItem(forecastDate, "1900", "POP", "10")
        );
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

    private KmaWeatherItem villageForecastItem(
            String forecastDate,
            String forecastTime,
            String category,
            String forecastValue
    ) {
        KmaWeatherItem item = new KmaWeatherItem();

        item.setBaseDate("20260621");
        item.setBaseTime("0800");
        item.setCategory(category);
        item.setFcstDate(forecastDate);
        item.setFcstTime(forecastTime);
        item.setFcstValue(forecastValue);

        return item;
    }
}
