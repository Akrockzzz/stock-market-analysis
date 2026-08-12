package com.stock.analysis.controller;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Fundamentals;
import com.stock.analysis.repository.FundamentalsRepository;
import com.stock.analysis.repository.InstrumentRepository;
import com.stock.analysis.service.FundamentalsSyncService;
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
    private final FundamentalsSyncService fundamentalsSyncService;
    private final InstrumentRepository instrumentRepository;

    @GetMapping("/{symbol}")
    public ResponseEntity<Fundamentals> getFundamentals(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();
        Fundamentals fundamentals = fundamentalsRepository.findBySymbol(upperSymbol).orElse(null);

        if (fundamentals == null) {
            var instrument = instrumentRepository.findBySymbolAndExchange(upperSymbol, Exchange.NSE_EQ).orElse(null);
            if (instrument != null) {
                fundamentals = fundamentalsSyncService.fetchAndStoreFundamentals(upperSymbol, instrument.getInstrumentKey());
            }
        }

        if (fundamentals == null) {
            throw new MarketDataUnavailableException(
                    upperSymbol,
                    "Fundamentals",
                    "No fundamentals financial profile recorded in database or Upstox REST API.");
        }

        return ResponseEntity.ok(fundamentals);
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
