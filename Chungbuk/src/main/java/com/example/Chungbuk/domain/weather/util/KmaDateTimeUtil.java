package com.example.Chungbuk.domain.weather.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class KmaDateTimeUtil {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmm");

    private KmaDateTimeUtil() {
    }

    public static KmaBaseDateTime getUltraSrtNcstBaseDateTime() {
        return getUltraSrtNcstBaseDateTime(
                LocalDateTime.now(KOREA_ZONE_ID)
        );
    }

    public static KmaBaseDateTime getUltraSrtNcstBaseDateTime(
            LocalDateTime currentDateTime
    ) {
        LocalDateTime baseDateTime = currentDateTime
                .minusMinutes(40)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        return createBaseDateTime(baseDateTime);
    }

    public static KmaBaseDateTime getUltraSrtFcstBaseDateTime() {
        return getUltraSrtFcstBaseDateTime(
                LocalDateTime.now(KOREA_ZONE_ID)
        );
    }

    public static KmaBaseDateTime getUltraSrtFcstBaseDateTime(
            LocalDateTime currentDateTime
    ) {
        LocalDateTime availableDateTime = currentDateTime.minusMinutes(45);

        LocalDateTime baseDateTime;

        if (availableDateTime.getMinute() < 30) {
            baseDateTime = availableDateTime
                    .minusHours(1)
                    .withMinute(30);
        } else {
            baseDateTime = availableDateTime.withMinute(30);
        }

        return createBaseDateTime(
                baseDateTime
                        .withSecond(0)
                        .withNano(0)
        );
    }

    private static KmaBaseDateTime createBaseDateTime(
            LocalDateTime baseDateTime
    ) {
        return new KmaBaseDateTime(
                baseDateTime.format(DATE_FORMATTER),
                baseDateTime.format(TIME_FORMATTER)
        );
    }
}