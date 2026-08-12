package com.stock.analysis.controller;

import com.stock.analysis.model.Fundamentals;
import com.stock.analysis.repository.FundamentalsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/fundamentals")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FundamentalsController {

    private final FundamentalsRepository fundamentalsRepository;

    @GetMapping("/{symbol}")
    public ResponseEntity<Fundamentals> getFundamentals(@PathVariable String symbol) {
        return fundamentalsRepository.findBySymbol(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(createSampleFundamentals(symbol.toUpperCase())));
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<Fundamentals> saveFundamentals(
            @PathVariable String symbol,
            @RequestBody Fundamentals fundamentals) {
        fundamentals.setSymbol(symbol.toUpperCase());
        fundamentals.setLastUpdated(Instant.now());
        Fundamentals saved = fundamentalsRepository.save(fundamentals);
        return ResponseEntity.ok(saved);
    }

    private Fundamentals createSampleFundamentals(String symbol) {
        return Fundamentals.builder()
                .symbol(symbol)
                .period("TTM (FY24)")
                .revenueInCr(12500.0)
                .revenueCagr5Yr(18.5)
                .netProfitInCr(2100.0)
                .ebitdaMarginPct(22.4)
                .netMarginPct(16.8)
                .roePct(21.2)
                .rocePct(24.6)
                .debtToEquity(0.12)
                .ocfInCr(2050.0)
                .fcfInCr(1600.0)
                .ocfToNetProfitRatio(0.98)
                .peRatio(24.5)
                .pbRatio(4.2)
                .evEbitda(16.2)
                .promoterHoldingPct(62.4)
                .promoterPledgePct(0.0)
                .fiiHoldingPct(18.2)
                .diiHoldingPct(12.1)
                .governanceFlagged(false)
                .lastUpdated(Instant.now())
                .build();
    }
}
