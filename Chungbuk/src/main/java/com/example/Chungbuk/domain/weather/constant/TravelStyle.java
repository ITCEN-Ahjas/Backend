package com.example.Chungbuk.domain.weather.constant;

import com.example.Chungbuk.global.exception.InvalidRequestException;

import java.util.Arrays;

public enum TravelStyle {

    DEFAULT("기본 추천"),
    WALKING("많이 걷는 여행"),
    OUTDOOR("야외 활동"),
    INDOOR("실내 중심"),
    NIGHT("야간 일정"),
    RAINY_DAY("비 오는 날 대비");

    private final String displayName;

    TravelStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TravelStyle fromDisplayName(String travelStyle) {
        if (travelStyle == null || travelStyle.isBlank()) {
            throw new InvalidRequestException(
                    "여행 스타일을 선택해 주세요."
            );
        }

        String normalizedTravelStyle = travelStyle.trim();

        return Arrays.stream(values())
                .filter(style ->
                        style.displayName.equals(normalizedTravelStyle)
                )
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "지원하지 않는 여행 스타일입니다."
                ));
    }
}