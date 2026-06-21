package com.example.Chungbuk.domain.weather.constant;

import java.util.Arrays;

public enum ChungbukRegion {

    CHEONGJU("청주", 69, 107),
    CHUNGJU("충주", 76, 115),
    JECHEON("제천", 81, 118),
    BOEUN("보은", 73, 104),
    OKCHEON("옥천", 71, 100),
    YEONGDONG("영동", 74, 97),
    JEUNGPYEONG("증평", 71, 110),
    JINCHEON("진천", 68, 111),
    GOESAN("괴산", 74, 111),
    EUMSEONG("음성", 72, 113),
    DANYANG("단양", 84, 115);

    private final String displayName;
    private final int nx;
    private final int ny;

    ChungbukRegion(String displayName, int nx, int ny) {
        this.displayName = displayName;
        this.nx = nx;
        this.ny = ny;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getNx() {
        return nx;
    }

    public int getNy() {
        return ny;
    }

    public static ChungbukRegion fromDisplayName(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            throw new IllegalArgumentException("충북 지역명은 필수입니다.");
        }

        String normalizedRegionName = regionName.trim();

        return Arrays.stream(values())
                .filter(region -> region.displayName.equals(normalizedRegionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 충북 지역입니다: " + normalizedRegionName
                ));
    }
}