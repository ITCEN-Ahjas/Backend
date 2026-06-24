package com.example.Chungbuk.global.exception;

public class ResidenceWeatherApiException extends RuntimeException {

    public ResidenceWeatherApiException(String message) {
        super(message);
    }

    public ResidenceWeatherApiException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
