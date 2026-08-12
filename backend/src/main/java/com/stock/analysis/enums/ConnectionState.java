package com.stock.analysis.enums;

public enum ConnectionState {
    LIVE("Live Market Feed Connected"),
    HISTORICAL_ONLY("Market Closed / Historical Data Mode"),
    NOT_CONNECTED("Upstox Connection Offline / API Key Required");

    private final String description;

    ConnectionState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
