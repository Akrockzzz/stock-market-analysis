package com.stock.analysis.engine;

import com.stock.analysis.enums.ScoringCategory;
import com.stock.analysis.model.StockScorecard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardEvaluationService {

    private final ObjectMapper objectMapper;

    public StockScorecard evaluateScorecard(String symbol, Map<ScoringCategory, Double> categoryRatings, String thesis, String exitRules, String notes) {
        double totalScore = 0.0;

        // Store raw 0-5 RATINGS (not weighted scores) so the frontend worksheet can reload slider values faithfully.
        // Weighted computation is done only for totalScore — never persisted to categoryScoresJson.
        Map<String, Double> rawRatingsMap = new HashMap<>();

        double maxWeightsSum = 0.0;
        for (ScoringCategory category : ScoringCategory.values()) {
            Double rating = categoryRatings != null ? categoryRatings.get(category) : 3.0;
            if (rating == null) rating = 3.0;
            // Clamp to valid 0.0 – 5.0 range
            rating = Math.max(0.0, Math.min(5.0, rating));
            double categoryScore = (rating / 5.0) * category.getWeight();
            totalScore += categoryScore;
            maxWeightsSum += category.getWeight();
            // Store raw rating (not weighted score) so frontend sliders are correct on reload
            rawRatingsMap.put(category.name(), Math.round(rating * 10.0) / 10.0);
        }

        // Normalize total score to scale of 50.0
        double scaledTotalScore = maxWeightsSum > 0 ? (totalScore / maxWeightsSum) * 50.0 : 0.0;
        scaledTotalScore = Math.round(scaledTotalScore * 100.0) / 100.0;
        String band = determineRecommendationBand(scaledTotalScore);

        String jsonScores;
        try {
            jsonScores = objectMapper.writeValueAsString(rawRatingsMap);
        } catch (Exception e) {
            log.error("Failed to serialize raw category ratings map", e);
            jsonScores = "{}";
        }

        return StockScorecard.builder()
                .symbol(symbol)
                .categoryScoresJson(jsonScores)
                .totalScore(scaledTotalScore)
                .maxPossibleScore(50.0)
                .recommendationBand(band)
                .investmentThesis(thesis)
                .exitCriteria(exitRules)
                .userNotes(notes)
                .updatedAt(Instant.now())
                .build();
    }

    public String determineRecommendationBand(double totalScore) {
        if (totalScore >= 40.0) {
            return "STRONG BUY CANDIDATE (40-50 pts)";
        } else if (totalScore >= 30.0) {
            return "HOLD / ACCUMULATE (30-39 pts)";
        } else {
            return "HIGH RISK / AVOID (<30 pts)";
        }
    }
}
