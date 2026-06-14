package com.example.Chungbuk.domain.festival.constant;

public enum FestivalCategory {

    ALL("전체"),
    CULTURE("문화축제"),
    FOOD("먹거리"),
    NATURE("자연체험"),
    ACTIVITY("액티비티"),
    NIGHT("야간행사"),
    MARKET("전통시장"),
    ETC("기타");

    private final String name;

    FestivalCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}