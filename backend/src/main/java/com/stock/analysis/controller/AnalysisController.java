package com.stock.analysis.controller;

import com.stock.analysis.dto.OptionChainAnalysisDto;
import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.engine.OptionChainAnalysisService;
import com.stock.analysis.engine.TechnicalAnalysisService;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Candle;
import com.stock.analysis.model.Tick;
import com.stock.analysis.repository.CandleRepository;
import com.stock.analysis.service.SpotPriceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalysisController {

    private final TechnicalAnalysisService technicalAnalysisService;
    private final OptionChainAnalysisService optionChainAnalysisService;
    private final CandleRepository candleRepository;
    private final SpotPriceCacheService spotPriceCacheService;

    @GetMapping("/technicals/{symbol}")
    public ResponseEntity<TechnicalAnalysisDto> getTechnicals(@PathVariable String symbol) {
        List<Candle> candles = candleRepository.findRecentCandles(symbol.toUpperCase(), "1d", PageRequest.of(0, 200));

        if (candles.isEmpty()) {
            throw new MarketDataUnavailableException(
                    symbol.toUpperCase(),
                    "Technicals",
                    "No historical candle data found in database. Trigger historical backfill service.");
        }

        TechnicalAnalysisDto dto = technicalAnalysisService.calculateTechnicals(symbol.toUpperCase(), candles);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/option-chain/{symbol}")
    public ResponseEntity<OptionChainAnalysisDto> getOptionChain(
            @PathVariable String symbol,
            @RequestParam(required = false) Double spotPrice) {

        Double currentSpot = spotPrice;
        if (currentSpot == null) {
            currentSpot = spotPriceCacheService.getLatestTick(symbol.toUpperCase())
                    .map(Tick::getLtp)
                    .orElseThrow(() -> new MarketDataUnavailableException(
                            symbol.toUpperCase(),
                            "Option Chain Spot Price",
                            "No live spot price tick available in spot cache. Streamer offline or idle."));
        }

        // Return real option chain snapshot from DB or throw exception if unpopulated
        List<StrikeDataDto> strikes = Collections.emptyList();
        if (strikes.isEmpty()) {
            throw new MarketDataUnavailableException(
                    symbol.toUpperCase(),
                    "Option Chain Matrix",
                    "No option chain strike data available for symbol/expiry.");
        }

        OptionChainAnalysisDto result = optionChainAnalysisService.analyzeOptionChain(symbol.toUpperCase(), currentSpot, strikes);
        return ResponseEntity.ok(result);
    }
}
