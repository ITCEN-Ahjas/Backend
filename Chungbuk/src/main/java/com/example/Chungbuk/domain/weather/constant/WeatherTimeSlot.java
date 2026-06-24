package com.example.Chungbuk.domain.weather.constant;

import java.time.LocalTime;

public enum WeatherTimeSlot {

    MORNING(
            "morning",
            "아침",
            LocalTime.of(8, 0),
            LocalTime.of(11, 0),
            LocalTime.of(9, 0)
    ),
    DAYTIME(
            "daytime",
            "낮",
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(12, 0)
    ),
    AFTERNOON(
            "afternoon",
            "오후",
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            LocalTime.of(15, 0)
    ),
    EVENING(
            "evening",
            "저녁",
            LocalTime.of(17, 0),
            LocalTime.of(21, 0),
            LocalTime.of(19, 0)
    );

    private final String code;
    private final String displayName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final LocalTime representativeTime;

    WeatherTimeSlot(
            String code,
            String displayName,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime representativeTime
    ) {
        this.code = code;
        this.displayName = displayName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.representativeTime = representativeTime;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalTime getRepresentativeTime() {
        return representativeTime;
    }

    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime)
                && time.isBefore(endTime);
    }
}
