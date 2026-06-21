package com.example.Chungbuk.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청 본문 형식이 올바르지 않습니다.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "지원하지 않는 요청값입니다.";

        if ("category".equals(exception.getName())) {
            message = "지원하지 않는 장소 카테고리입니다.";
        }

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                message,
                request
        );
    }

    @ExceptionHandler(GooglePlacesApiException.class)
    public ResponseEntity<ErrorResponse> handleGooglePlacesApiException(
            GooglePlacesApiException exception,
            HttpServletRequest request
    ) {
        return createErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "GOOGLE_PLACES_API_ERROR",
                "장소 검색 서비스에 일시적으로 연결할 수 없습니다.",
                request
        );
    }

    @ExceptionHandler(KmaWeatherApiException.class)
    public ResponseEntity<ErrorResponse> handleKmaWeatherApiException(
            KmaWeatherApiException exception,
            HttpServletRequest request
    ) {
        return createErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "KMA_WEATHER_API_ERROR",
                "날씨 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}