package com.example.Chungbuk.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
