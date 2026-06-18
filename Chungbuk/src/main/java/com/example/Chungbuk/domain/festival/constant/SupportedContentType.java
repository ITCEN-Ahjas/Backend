package com.example.Chungbuk.domain.festival.constant;

import java.util.Arrays;

public enum SupportedContentType {

    TOURIST_ATTRACTION("12", "관광지"),
    CULTURAL_FACILITY("14", "문화시설"),
    EVENT_PERFORMANCE_FESTIVAL("15", "행사"),
    LEPORTS("28", "레포츠"),

    UNKNOWN("", "기타");

    private final String contentTypeId;
    private final String defaultCategory;

    SupportedContentType(String contentTypeId, String defaultCategory) {
        this.contentTypeId = contentTypeId;
        this.defaultCategory = defaultCategory;
    }

    public String getContentTypeId() {
        return contentTypeId;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public static SupportedContentType from(String contentTypeId) {
        if (contentTypeId == null || contentTypeId.isBlank()) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(type -> type.contentTypeId.equals(contentTypeId.trim()))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static boolean isSupported(String contentTypeId) {
        return from(contentTypeId) != UNKNOWN;
    }

    public static boolean isExperienceSupported(String contentTypeId) {
        return TOURIST_ATTRACTION.contentTypeId.equals(contentTypeId)
                || CULTURAL_FACILITY.contentTypeId.equals(contentTypeId)
                || LEPORTS.contentTypeId.equals(contentTypeId);
    }

    public static String resolveDefaultCategory(String contentTypeId) {
        return from(contentTypeId).getDefaultCategory();
    }

    public static String resolveExperienceContentTypeId(String value) {
        if (value == null || value.isBlank() || "전체".equals(value.trim())) {
            return "";
        }

        String trimmedValue = value.trim();

        if ("12".equals(trimmedValue) || "관광지".equals(trimmedValue)) {
            return TOURIST_ATTRACTION.contentTypeId;
        }

        if ("14".equals(trimmedValue) || "문화시설".equals(trimmedValue)) {
            return CULTURAL_FACILITY.contentTypeId;
        }

        if ("28".equals(trimmedValue) || "레포츠".equals(trimmedValue)) {
            return LEPORTS.contentTypeId;
        }

        return "__UNSUPPORTED__";
    }
}