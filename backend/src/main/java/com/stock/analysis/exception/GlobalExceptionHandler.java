package com.stock.analysis.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleMarketDataUnavailable(MarketDataUnavailableException ex) {
        ErrorResponseDto error = new ErrorResponseDto(
                "MARKET_DATA_UNAVAILABLE",
                ex.getSymbol(),
                ex.getMetric(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UpstoxApiException.class)
    public ResponseEntity<ErrorResponseDto> handleUpstoxApiException(UpstoxApiException ex) {
        ErrorResponseDto error = new ErrorResponseDto(
                "UPSTOX_API_ERROR",
                "N/A",
                "Upstox API",
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode())).body(error);
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponseDto {
        private String errorCode;
        private String symbol;
        private String metric;
        private String message;
        private Instant timestamp;
    }
}
