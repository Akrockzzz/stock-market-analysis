package com.stock.analysis.controller;

import com.stock.analysis.exception.MarketDataUnavailableException;
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
                .orElseThrow(() -> new MarketDataUnavailableException(
                        symbol.toUpperCase(),
                        "Fundamentals",
                        "No fundamentals financial profile recorded in database for symbol. Sync from Upstox or enter manually."));
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
}
