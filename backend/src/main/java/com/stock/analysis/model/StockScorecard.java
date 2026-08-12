package com.stock.analysis.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_scorecards")
public class StockScorecard {

    @Id
    private String symbol;

    @Column(columnDefinition = "TEXT")
    private String categoryScoresJson; // JSON map of ScoringCategory -> Double (0.0 to 5.0)

    private Double totalScore; // Sum scaled to max 50 points
    private Double maxPossibleScore; // 50.0

    private String recommendationBand; // e.g. "STRONG BUY CANDIDATE (40-50 pts)", "HOLD / ACCUMULATE (30-39 pts)", "HIGH RISK / AVOID (<30 pts)"
    
    @Column(columnDefinition = "TEXT")
    private String investmentThesis;

    @Column(columnDefinition = "TEXT")
    private String exitCriteria;

    @Column(columnDefinition = "TEXT")
    private String userNotes;

    private Instant updatedAt;
}
