package com.stock.analysis.exception;

public class UpstoxApiException extends RuntimeException {
    private final int statusCode;

    public UpstoxApiException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public UpstoxApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
