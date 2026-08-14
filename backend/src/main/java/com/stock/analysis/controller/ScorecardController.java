package com.stock.analysis.controller;

import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.engine.ScorecardEvaluationService;
import com.stock.analysis.enums.ScoringCategory;
import com.stock.analysis.model.StockScorecard;
import com.stock.analysis.repository.StockScorecardRepository;
import com.stock.analysis.service.ScorecardAutoSuggestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scorecard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ScorecardController {

    private final ScorecardEvaluationService scorecardEvaluationService;
    private final StockScorecardRepository stockScorecardRepository;
    private final ScorecardAutoSuggestService scorecardAutoSuggestService;

    @GetMapping("/{symbol}")
    public ResponseEntity<StockScorecard> getScorecard(@PathVariable String symbol) {
        return stockScorecardRepository.findBySymbol(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/auto-suggest")
    public ResponseEntity<Map<String, Object>> getAutoSuggestedScorecard(
            @PathVariable String symbol,
            @RequestBody(required = false) TechnicalAnalysisDto technicals) {

        String upperSymbol = symbol.toUpperCase();
        Map<ScoringCategory, Double> suggestedRatings = scorecardAutoSuggestService.generateAutoSuggestions(upperSymbol, technicals);

        StockScorecard calculated = scorecardEvaluationService.evaluateScorecard(
                upperSymbol,
                suggestedRatings,
                "Auto-generated baseline thesis based on recorded fundamentals and technical indicators.",
                "1. Exit if revenue CAGR falls below 8%. 2. Exit if 20 EMA breaks below 200 EMA.",
                "Pre-filled by system baseline auto-suggest service."
        );

        Map<String, Double> stringKeyedRatings = new HashMap<>();
        suggestedRatings.forEach((k, v) -> stringKeyedRatings.put(k.name(), v));

        Map<String, Object> response = new HashMap<>();
        response.put("symbol", upperSymbol);
        response.put("categoryRatings", stringKeyedRatings);
        response.put("totalScore", calculated.getTotalScore());
        response.put("maxPossibleScore", calculated.getMaxPossibleScore());
        response.put("recommendationBand", calculated.getRecommendationBand());
        response.put("investmentThesis", calculated.getInvestmentThesis());
        response.put("exitCriteria", calculated.getExitCriteria());
        response.put("userNotes", calculated.getUserNotes());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<StockScorecard> evaluateAndSaveScorecard(
            @PathVariable String symbol,
            @RequestBody ScorecardRequest request) {
        StockScorecard scorecard = scorecardEvaluationService.evaluateScorecard(
                symbol.toUpperCase(),
                request.getCategoryRatings(),
                request.getInvestmentThesis(),
                request.getExitCriteria(),
                request.getUserNotes()
        );
        StockScorecard saved = stockScorecardRepository.save(scorecard);
        return ResponseEntity.ok(saved);
    }

    @Data
    public static class ScorecardRequest {
        private Map<ScoringCategory, Double> categoryRatings;
        private String investmentThesis;
        private String exitCriteria;
        private String userNotes;
    }
}
