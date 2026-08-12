package com.stock.analysis.engine;

import com.stock.analysis.enums.ScoringCategory;
import com.stock.analysis.model.StockScorecard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScorecardEvaluationServiceTest {

    private ScorecardEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new ScorecardEvaluationService(new ObjectMapper());
    }

    @Test
    void shouldEvaluatePerfectScorecardAsStrongBuy() {
        Map<ScoringCategory, Double> ratings = new HashMap<>();
        for (ScoringCategory cat : ScoringCategory.values()) {
            ratings.put(cat, 5.0); // Perfect rating 5/5
        }

        StockScorecard scorecard = service.evaluateScorecard("TCS", ratings, "Solid business", "N/A", "Looks strong");

        assertNotNull(scorecard);
        assertEquals(50.0, scorecard.getTotalScore());
        assertEquals("STRONG BUY CANDIDATE (40-50 pts)", scorecard.getRecommendationBand());
    }

    @Test
    void shouldEvaluateLowScorecardAsHighRisk() {
        Map<ScoringCategory, Double> ratings = new HashMap<>();
        for (ScoringCategory cat : ScoringCategory.values()) {
            ratings.put(cat, 2.0); // Rating 2/5 -> Total score 20.0
        }

        StockScorecard scorecard = service.evaluateScorecard("WEAKSTK", ratings, "Risky business", "Exit immediately", "Avoid");

        assertNotNull(scorecard);
        assertEquals(20.0, scorecard.getTotalScore());
        assertEquals("HIGH RISK / AVOID (<30 pts)", scorecard.getRecommendationBand());
    }
}
