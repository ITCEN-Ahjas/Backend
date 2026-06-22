package com.example.Chungbuk.global.exception;

public class AiOutfitApiException extends RuntimeException {

    public AiOutfitApiException(String message) {
        super(message);
    }

    public AiOutfitApiException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}