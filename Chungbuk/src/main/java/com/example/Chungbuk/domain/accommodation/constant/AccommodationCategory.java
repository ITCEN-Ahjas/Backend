package com.example.Chungbuk.domain.accommodation.constant;

public enum AccommodationCategory {

    HOTEL("호텔"),
    MOTEL("모텔"),
    PENSION("펜션"),
    GUESTHOUSE("게스트하우스"),
    HANOK("한옥"),
    CAMPING("캠핑/글램핑"),
    RESORT("리조트/콘도"),
    ETC("기타 숙박");

    private final String displayName;

    AccommodationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AccommodationCategory resolve(String cat3, String title) {
        String merged = normalize(cat3 + " " + title);

        if (containsAny(merged, "b02011600", "펜션")) {
            return PENSION;
        }

        if (containsAny(merged, "b02011700", "캠핑", "글램핑", "야영", "카라반")) {
            return CAMPING;
        }

        if (containsAny(merged, "b02011800", "게스트하우스", "민박")) {
            return GUESTHOUSE;
        }

        if (containsAny(merged, "b02012600", "한옥")) {
            return HANOK;
        }

        if (containsAny(merged, "b02011500", "리조트", "콘도")) {
            return RESORT;
        }

        if (containsAny(merged, "b02010100", "호텔")) {
            return HOTEL;
        }

        if (containsAny(merged, "b02010900", "모텔")) {
            return MOTEL;
        }

        return ETC;
    }

    private static boolean containsAny(String value, String... keywords) {
        if (value == null || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (keyword != null && value.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replace(" ", "").toLowerCase();
    }
}
