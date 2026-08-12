package com.stock.analysis.engine;

import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Candle;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TechnicalAnalysisService {

    public TechnicalAnalysisDto calculateTechnicals(String symbol, List<Candle> candles) {
        if (candles == null || candles.size() < 14) {
            throw new MarketDataUnavailableException(
                    symbol, "Technicals", "Insufficient candle history. Minimum 14 candles required for indicator calculation.");
        }

        List<Double> closePrices = candles.stream().map(Candle::getClose).toList();

        Double ema20 = candles.size() >= 20 ? calculateEMA(closePrices, 20) : null;
        Double ema50 = candles.size() >= 50 ? calculateEMA(closePrices, 50) : null;
        Double ema200 = candles.size() >= 200 ? calculateEMA(closePrices, 200) : null;

        Double rsi14 = calculateRSI(closePrices, 14);

        TechnicalAnalysisDto.MacdResult macd = calculateMACD(closePrices);
        TechnicalAnalysisDto.PivotLevels pivots = calculatePivotLevels(candles);

        return TechnicalAnalysisDto.builder()
                .symbol(symbol)
                .lastPrice(closePrices.get(closePrices.size() - 1))
                .ema20(ema20)
                .ema50(ema50)
                .ema200(ema200)
                .rsi14(rsi14)
                .macdLine(macd.getMacd())
                .macdSignal(macd.getSignal())
                .macdHistogram(macd.getHistogram())
                .support1(pivots.getSupport1())
                .support2(pivots.getSupport2())
                .resistance1(pivots.getResistance1())
                .resistance2(pivots.getResistance2())
                .pivotPoint(pivots.getPivot())
                .build();
    }

    public Double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) return null;
        double multiplier = 2.0 / (period + 1);

        // Initial SMA seed
        double ema = prices.subList(0, period).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return Math.round(ema * 100.0) / 100.0;
    }

    public Double calculateRSI(List<Double> prices, int period) {
        if (prices.size() <= period) return null;

        double gains = 0;
        double losses = 0;

        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) gains += change;
            else losses += Math.abs(change);
        }

        double avgGain = gains / period;
        double avgLoss = losses / period;

        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                avgGain = (avgGain * (period - 1) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgGain = (avgGain * (period - 1)) / period;
                avgLoss = (avgLoss * (period - 1) + Math.abs(change)) / period;
            }
        }

        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        return Math.round(rsi * 100.0) / 100.0;
    }

    public TechnicalAnalysisDto.MacdResult calculateMACD(List<Double> prices) {
        if (prices.size() < 26) {
            return new TechnicalAnalysisDto.MacdResult(null, null, null);
        }
        Double ema12 = calculateEMA(prices, 12);
        Double ema26 = calculateEMA(prices, 26);
        if (ema12 == null || ema26 == null) return new TechnicalAnalysisDto.MacdResult(null, null, null);

        double macd = ema12 - ema26;
        double signal = macd * 0.2; // Simplified signal smoothing
        double histogram = macd - signal;

        return new TechnicalAnalysisDto.MacdResult(
                Math.round(macd * 100.0) / 100.0,
                Math.round(signal * 100.0) / 100.0,
                Math.round(histogram * 100.0) / 100.0);
    }

    public TechnicalAnalysisDto.PivotLevels calculatePivotLevels(List<Candle> candles) {
        Candle last = candles.get(candles.size() - 1);
        double high = last.getHigh();
        double low = last.getLow();
        double close = last.getClose();

        double pivot = (high + low + close) / 3.0;
        double r1 = (2 * pivot) - low;
        double s1 = (2 * pivot) - high;
        double r2 = pivot + (high - low);
        double s2 = pivot - (high - low);

        return new TechnicalAnalysisDto.PivotLevels(
                Math.round(pivot * 100.0) / 100.0,
                Math.round(r1 * 100.0) / 100.0,
                Math.round(r2 * 100.0) / 100.0,
                Math.round(s1 * 100.0) / 100.0,
                Math.round(s2 * 100.0) / 100.0);
    }
}
