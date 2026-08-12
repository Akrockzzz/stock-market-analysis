package com.stock.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysisDto {
    private String symbol;
    private Double lastPrice;

    private Double ema20;
    private Double ema50;
    private Double ema200;

    private Double rsi14;

    private Double macdLine;
    private Double macdSignal;
    private Double macdHistogram;

    private Double pivotPoint;
    private Double resistance1;
    private Double resistance2;
    private Double support1;
    private Double support2;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MacdResult {
        private Double macd;
        private Double signal;
        private Double histogram;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PivotLevels {
        private Double pivot;
        private Double resistance1;
        private Double resistance2;
        private Double support1;
        private Double support2;
    }
}
