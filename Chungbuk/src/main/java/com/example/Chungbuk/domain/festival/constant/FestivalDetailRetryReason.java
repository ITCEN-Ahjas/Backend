package com.example.Chungbuk.domain.festival.constant;

public enum FestivalDetailRetryReason {

    DETAIL_COMMON_EMPTY(false),
    DETAIL_COMMON_REQUEST_FAILED(false),
    DETAIL_INTRO_REQUEST_FAILED(false),
    DETAIL_IMAGE_REQUEST_FAILED(true),
    DETAIL_RESPONSE_INVALID(false),
    DETAIL_SAVE_FAILED(false);

    private final boolean imageOnlyRetry;

    FestivalDetailRetryReason(boolean imageOnlyRetry) {
        this.imageOnlyRetry = imageOnlyRetry;
    }

    public boolean isImageOnlyRetry() {
        return imageOnlyRetry;
    }

    public static FestivalDetailRetryReason from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return FestivalDetailRetryReason.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}