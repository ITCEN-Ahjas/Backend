package com.example.Chungbuk.global.exception;

import org.springframework.web.client.RestClientException;

public class TourApiQuotaExceededException extends RestClientException {

    public TourApiQuotaExceededException(String message) {
        super(message);
    }
}