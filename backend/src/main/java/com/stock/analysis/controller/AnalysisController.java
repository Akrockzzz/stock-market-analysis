package com.stock.analysis.controller;

import com.stock.analysis.dto.OptionChainAnalysisDto;
import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.engine.OptionChainAnalysisService;
import com.stock.analysis.engine.TechnicalAnalysisService;
import com.stock.analysis.enums.Exchange;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Candle;
import com.stock.analysis.model.OptionChainSnapshot;
import com.stock.analysis.model.Tick;
import com.stock.analysis.repository.CandleRepository;
import com.stock.analysis.repository.InstrumentRepository;
import com.stock.analysis.repository.OptionChainSnapshotRepository;
import com.stock.analysis.service.HistoricalDataSyncService;
import com.stock.analysis.service.OptionChainSyncService;
import com.stock.analysis.service.SpotPriceCacheService;
import com.stock.analysis.util.ExpiryUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalysisController {

    private final TechnicalAnalysisService technicalAnalysisService;
    private final OptionChainAnalysisService optionChainAnalysisService;
    private final CandleRepository candleRepository;
    private final SpotPriceCacheService spotPriceCacheService;
    private final HistoricalDataSyncService historicalDataSyncService;
    private final OptionChainSyncService optionChainSyncService;
    private final InstrumentRepository instrumentRepository;
    private final OptionChainSnapshotRepository optionChainSnapshotRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/technicals/{symbol}")
    public ResponseEntity<TechnicalAnalysisDto> getTechnicals(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();
        List<Candle> candles = new java.util.ArrayList<>(candleRepository.findRecentCandles(upperSymbol, "1d", PageRequest.of(0, 200)));

        if (candles.isEmpty()) {
            instrumentRepository.findBySymbolAndExchange(upperSymbol, Exchange.NSE_EQ)
                    .ifPresent(instrument -> historicalDataSyncService.fetchAndStoreHistoricalCandles(
                            upperSymbol, instrument.getInstrumentKey(), "1d", LocalDate.now().minusYears(1), LocalDate.now()));

            candles = new java.util.ArrayList<>(candleRepository.findRecentCandles(upperSymbol, "1d", PageRequest.of(0, 200)));
        }

        if (candles.isEmpty()) {
            throw new MarketDataUnavailableException(
                    upperSymbol,
                    "Technicals",
                    "No historical candle data available in database or Upstox REST API.");
        }

        List<Candle> distinctCandles = new java.util.ArrayList<>(
                candles.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Candle::getTimestamp,
                                c -> c,
                                (existing, replacement) -> existing,
                                java.util.LinkedHashMap::new
                        ))
                        .values()
        );
        java.util.Collections.reverse(distinctCandles);
        TechnicalAnalysisDto dto = technicalAnalysisService.calculateTechnicals(upperSymbol, distinctCandles);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/option-chain/{symbol}")
    public ResponseEntity<OptionChainAnalysisDto> getOptionChain(
            @PathVariable String symbol,
            @RequestParam(required = false) Double spotPrice,
            @RequestParam(required = false) String expiry) {

        String upperSymbol = symbol.toUpperCase();
        Double currentSpot = spotPrice;
        if (currentSpot == null) {
            currentSpot = spotPriceCacheService.getLatestTick(upperSymbol)
                    .map(Tick::getLtp)
                    .orElse(null);
        }

        if (currentSpot == null) {
            List<Candle> recentCandles = candleRepository.findRecentCandles(upperSymbol, "1d", PageRequest.of(0, 1));
            if (!recentCandles.isEmpty()) {
                currentSpot = recentCandles.get(0).getClose();
            }
        }

        LocalDate expiryDate = expiry != null ? LocalDate.parse(expiry) : ExpiryUtil.getNextValidExpiryDate(upperSymbol, Exchange.NSE_EQ);

        OptionChainSnapshot snapshot = optionChainSnapshotRepository
                .findFirstByUnderlyingSymbolAndExpiryDateOrderByTimestampDesc(upperSymbol, expiryDate)
                .orElse(null);

        if (snapshot == null && currentSpot != null) {
            var instrument = instrumentRepository.findBySymbolAndExchange(upperSymbol, Exchange.NSE_EQ).orElse(null);
            if (instrument != null) {
                snapshot = optionChainSyncService.fetchAndStoreOptionChain(upperSymbol, instrument.getInstrumentKey(), expiryDate, currentSpot);
            }
        }

        if (snapshot == null) {
            throw new MarketDataUnavailableException(
                    upperSymbol,
                    "Option Chain Matrix",
                    "No real option chain snapshot found in database or Upstox REST API for expiry " + expiryDate + ".");
        }

        List<StrikeDataDto> strikes = Collections.emptyList();
        try {
            if (snapshot.getJsonStrikeData() != null) {
                strikes = objectMapper.readValue(snapshot.getJsonStrikeData(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.error("Error parsing snapshot strike JSON", e);
        }

        OptionChainAnalysisDto result = optionChainAnalysisService.analyzeOptionChain(upperSymbol, snapshot.getSpotPrice(), strikes);
        return ResponseEntity.ok(result);
    }
}
