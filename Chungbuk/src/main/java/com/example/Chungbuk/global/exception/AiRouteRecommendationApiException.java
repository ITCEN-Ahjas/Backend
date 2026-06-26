package com.example.Chungbuk.global.exception;

public class AiRouteRecommendationApiException extends RuntimeException {

    public AiRouteRecommendationApiException(String message) {
        super(message);
    }

    public AiRouteRecommendationApiException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
