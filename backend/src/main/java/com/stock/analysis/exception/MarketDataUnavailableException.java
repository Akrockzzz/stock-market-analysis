package com.stock.analysis.exception;

public class MarketDataUnavailableException extends RuntimeException {
    private final String symbol;
    private final String metric;

    public MarketDataUnavailableException(String symbol, String metric, String reason) {
        super(String.format("Market data unavailable for symbol '%s' when calculating '%s': %s", symbol, metric, reason));
        this.symbol = symbol;
        this.metric = metric;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMetric() {
        return metric;
    }
}
