package com.stock.analysis.controller;

import com.stock.analysis.engine.ScorecardEvaluationService;
import com.stock.analysis.enums.ScoringCategory;
import com.stock.analysis.model.StockScorecard;
import com.stock.analysis.repository.StockScorecardRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scorecard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ScorecardController {

    private final ScorecardEvaluationService scorecardEvaluationService;
    private final StockScorecardRepository stockScorecardRepository;

    @GetMapping("/{symbol}")
    public ResponseEntity<StockScorecard> getScorecard(@PathVariable String symbol) {
        return stockScorecardRepository.findBySymbol(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
