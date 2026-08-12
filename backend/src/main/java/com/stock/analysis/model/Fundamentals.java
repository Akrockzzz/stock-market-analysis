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
@Table(name = "fundamentals")
public class Fundamentals {

    @Id
    private String symbol;

    private String period; // TTM / FY24 / Q4FY24
    private Double revenueInCr;
    private Double revenueCagr5Yr;
    private Double netProfitInCr;
    private Double ebitdaMarginPct;
    private Double netMarginPct;
    private Double roePct;
    private Double rocePct;
    private Double debtToEquity;
    private Double ocfInCr;
    private Double fcfInCr;
    private Double ocfToNetProfitRatio;
    private Double peRatio;
    private Double pbRatio;
    private Double evEbitda;
    private Double promoterHoldingPct;
    private Double promoterPledgePct;
    private Double fiiHoldingPct;
    private Double diiHoldingPct;
    private Boolean governanceFlagged;

    private Instant lastUpdated;
}
