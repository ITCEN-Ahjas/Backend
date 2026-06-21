package com.example.Chungbuk.domain.weather.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KmaDateTimeUtilTest {

    @Test
    @DisplayName("초단기실황은 현재 시각보다 40분 전 기준의 정시를 사용한다")
    void getUltraSrtNcstBaseDateTime_returnsAvailableHour() {
        LocalDateTime currentDateTime =
                LocalDateTime.of(2026, 6, 21, 0, 35);

        KmaBaseDateTime result =
                KmaDateTimeUtil.getUltraSrtNcstBaseDateTime(
                        currentDateTime
                );

        assertEquals("20260620", result.baseDate());
        assertEquals("2300", result.baseTime());
    }

    @Test
    @DisplayName("초단기예보는 현재 시각보다 45분 전 기준의 30분 발표 시각을 사용한다")
    void getUltraSrtFcstBaseDateTime_returnsAvailableForecastTime() {
        LocalDateTime currentDateTime =
                LocalDateTime.of(2026, 6, 21, 13, 40);

        KmaBaseDateTime result =
                KmaDateTimeUtil.getUltraSrtFcstBaseDateTime(
                        currentDateTime
                );

        assertEquals("20260621", result.baseDate());
        assertEquals("1230", result.baseTime());
    }

    @Test
    @DisplayName("초단기예보 발표 시각이 전날로 넘어가면 날짜도 함께 변경한다")
    void getUltraSrtFcstBaseDateTime_changesDateAtMidnight() {
        LocalDateTime currentDateTime =
                LocalDateTime.of(2026, 6, 21, 0, 10);

        KmaBaseDateTime result =
                KmaDateTimeUtil.getUltraSrtFcstBaseDateTime(
                        currentDateTime
                );

        assertEquals("20260620", result.baseDate());
        assertEquals("2230", result.baseTime());
    }
}