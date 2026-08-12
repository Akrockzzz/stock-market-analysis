package com.stock.analysis.service;

import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.enums.ScoringCategory;
import com.stock.analysis.model.Fundamentals;
import com.stock.analysis.repository.FundamentalsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardAutoSuggestService {

    private final FundamentalsRepository fundamentalsRepository;

    public Map<ScoringCategory, Double> generateAutoSuggestions(String symbol, TechnicalAnalysisDto technicals) {
        Map<ScoringCategory, Double> suggestions = new EnumMap<>(ScoringCategory.class);

        // Default baseline rating 3.0 (Neutral) across all 22 categories
        for (ScoringCategory category : ScoringCategory.values()) {
            suggestions.put(category, 3.0);
        }

        // Evaluate Fundamentals if available
        fundamentalsRepository.findBySymbol(symbol.toUpperCase()).ifPresent(f -> {
            // Category 2: Revenue Growth
            if (f.getRevenueCagr5Yr() != null) {
                if (f.getRevenueCagr5Yr() >= 15.0) suggestions.put(ScoringCategory.REVENUE_GROWTH, 5.0);
                else if (f.getRevenueCagr5Yr() >= 10.0) suggestions.put(ScoringCategory.REVENUE_GROWTH, 4.0);
                else if (f.getRevenueCagr5Yr() < 5.0) suggestions.put(ScoringCategory.REVENUE_GROWTH, 2.0);
            }

            // Category 3: Margins
            if (f.getEbitdaMarginPct() != null) {
                if (f.getEbitdaMarginPct() >= 20.0) suggestions.put(ScoringCategory.PROFIT_MARGINS, 5.0);
                else if (f.getEbitdaMarginPct() >= 15.0) suggestions.put(ScoringCategory.PROFIT_MARGINS, 4.0);
                else if (f.getEbitdaMarginPct() < 10.0) suggestions.put(ScoringCategory.PROFIT_MARGINS, 2.0);
            }

            // Category 4: ROE & ROCE
            if (f.getRoePct() != null && f.getRocePct() != null) {
                if (f.getRoePct() >= 18.0 && f.getRocePct() >= 20.0) suggestions.put(ScoringCategory.ROE_ROCE, 5.0);
                else if (f.getRoePct() >= 15.0 && f.getRocePct() >= 15.0) suggestions.put(ScoringCategory.ROE_ROCE, 4.0);
                else if (f.getRoePct() < 10.0) suggestions.put(ScoringCategory.ROE_ROCE, 2.0);
            }

            // Category 5: Debt to Equity
            if (f.getDebtToEquity() != null) {
                if (f.getDebtToEquity() <= 0.1) suggestions.put(ScoringCategory.BALANCE_SHEET_DEBT, 5.0);
                else if (f.getDebtToEquity() <= 0.5) suggestions.put(ScoringCategory.BALANCE_SHEET_DEBT, 4.0);
                else if (f.getDebtToEquity() > 1.0) suggestions.put(ScoringCategory.BALANCE_SHEET_DEBT, 1.5);
            }

            // Category 9: Promoter Pledge
            if (f.getPromoterPledgePct() != null) {
                if (f.getPromoterPledgePct() == 0.0) suggestions.put(ScoringCategory.PROMOTER_HOLDING_PLEDGE, 5.0);
                else if (f.getPromoterPledgePct() > 10.0) suggestions.put(ScoringCategory.PROMOTER_HOLDING_PLEDGE, 1.0);
            }
        });

        // Evaluate Technicals if available
        if (technicals != null) {
            // Category 17: Moving Average Alignment
            if (technicals.getEma20() != null && technicals.getEma50() != null && technicals.getEma200() != null) {
                if (technicals.getEma20() > technicals.getEma50() && technicals.getEma50() > technicals.getEma200()) {
                    suggestions.put(ScoringCategory.TECHNICAL_TREND_MOVING_AVERAGES, 5.0);
                } else if (technicals.getEma20() < technicals.getEma50()) {
                    suggestions.put(ScoringCategory.TECHNICAL_TREND_MOVING_AVERAGES, 2.0);
                }
            }

            // Category 18: RSI Momentum
            if (technicals.getRsi14() != null) {
                if (technicals.getRsi14() >= 45.0 && technicals.getRsi14() <= 65.0) {
                    suggestions.put(ScoringCategory.MOMENTUM_INDICATORS, 5.0);
                } else if (technicals.getRsi14() > 75.0 || technicals.getRsi14() < 30.0) {
                    suggestions.put(ScoringCategory.MOMENTUM_INDICATORS, 2.5);
                }
            }
        }

        return suggestions;
    }
}
