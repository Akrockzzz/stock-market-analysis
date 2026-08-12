package com.stock.analysis.engine;

import com.stock.analysis.dto.OptionChainAnalysisDto;
import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.exception.MarketDataUnavailableException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class OptionChainAnalysisService {

    public OptionChainAnalysisDto analyzeOptionChain(String symbol, Double spotPrice, List<StrikeDataDto> strikes) {
        if (spotPrice == null || spotPrice <= 0) {
            throw new MarketDataUnavailableException(symbol, "Option Chain", "Invalid spot price: " + spotPrice);
        }
        if (strikes == null || strikes.isEmpty()) {
            throw new MarketDataUnavailableException(symbol, "Option Chain", "No strike data available for expiry.");
        }

        long totalCallOi = strikes.stream().mapToLong(s -> s.getCallOi() != null ? s.getCallOi() : 0L).sum();
        long totalPutOi = strikes.stream().mapToLong(s -> s.getPutOi() != null ? s.getPutOi() : 0L).sum();

        if (totalCallOi == 0) {
            throw new MarketDataUnavailableException(symbol, "PCR", "Total Call Open Interest is 0. Cannot compute PCR.");
        }

        double pcr = Math.round(((double) totalPutOi / totalCallOi) * 100.0) / 100.0;

        // ATM Strike Calculation
        StrikeDataDto atmStrikeDto = strikes.stream()
                .min(Comparator.comparingDouble(s -> Math.abs(s.getStrikePrice() - spotPrice)))
                .orElseThrow(() -> new MarketDataUnavailableException(symbol, "ATM Strike", "Failed to compute ATM strike"));

        double atmStrike = atmStrikeDto.getStrikePrice();

        // Max Pain Strike Calculation
        double maxPainStrike = calculateMaxPain(strikes);

        return OptionChainAnalysisDto.builder()
                .underlyingSymbol(symbol)
                .spotPrice(spotPrice)
                .atmStrike(atmStrike)
                .totalCallOi(totalCallOi)
                .totalPutOi(totalPutOi)
                .putCallRatio(pcr)
                .maxPainStrike(maxPainStrike)
                .strikes(strikes)
                .build();
    }

    public double calculateMaxPain(List<StrikeDataDto> strikes) {
        double minTotalLoss = Double.MAX_VALUE;
        double maxPainStrike = strikes.get(0).getStrikePrice();

        for (StrikeDataDto evalStrike : strikes) {
            double currentEvalPrice = evalStrike.getStrikePrice();
            double totalWriterPayout = 0.0;

            for (StrikeDataDto strike : strikes) {
                // Call Option Buyer Payoff if market expires at currentEvalPrice
                if (currentEvalPrice > strike.getStrikePrice() && strike.getCallOi() != null) {
                    totalWriterPayout += (currentEvalPrice - strike.getStrikePrice()) * strike.getCallOi();
                }
                // Put Option Buyer Payoff if market expires at currentEvalPrice
                if (currentEvalPrice < strike.getStrikePrice() && strike.getPutOi() != null) {
                    totalWriterPayout += (strike.getStrikePrice() - currentEvalPrice) * strike.getPutOi();
                }
            }

            if (totalWriterPayout < minTotalLoss) {
                minTotalLoss = totalWriterPayout;
                maxPainStrike = currentEvalPrice;
            }
        }
        return maxPainStrike;
    }
}
