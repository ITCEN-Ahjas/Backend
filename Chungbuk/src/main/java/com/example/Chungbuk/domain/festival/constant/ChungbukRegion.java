package com.example.Chungbuk.domain.festival.constant;

import java.util.Arrays;

public enum ChungbukRegion {

    ALL("전체", null),
    GOESAN("괴산", "1"),
    DANYANG("단양", "2"),
    BOEUN("보은", "3"),
    YEONGDONG("영동", "4"),
    OKCHEON("옥천", "5"),
    EUMSEONG("음성", "6"),
    JECHEON("제천", "7"),
    JINCHEON("진천", "8"),
    CHEONGJU("청주", "10"),
    CHUNGJU("충주", "11"),
    JEUNGPYEONG("증평", "12");

    private final String name;
    private final String sigunguCode;

    ChungbukRegion(String name, String sigunguCode) {
        this.name = name;
        this.sigunguCode = sigunguCode;
    }

    public String getName() {
        return name;
    }

    public String getSigunguCode() {
        return sigunguCode;
    }

    public static String findSigunguCodeByName(String regionName) {
        if (regionName == null || regionName.isBlank() || regionName.equals("전체")) {
            return null;
        }

        return Arrays.stream(values())
                .filter(region -> region.name.equals(regionName))
                .map(ChungbukRegion::getSigunguCode)
                .findFirst()
                .orElse(null);
    }
}