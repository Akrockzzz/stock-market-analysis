package com.stock.analysis.controller;

import com.stock.analysis.dto.ConnectionStatusDto;
import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
import com.stock.analysis.model.Candle;
import com.stock.analysis.model.Tick;
import com.stock.analysis.repository.CandleRepository;
import com.stock.analysis.service.SpotPriceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MarketDataController {

    private final UpstoxWebSocketStreamer webSocketStreamer;
    private final SpotPriceCacheService spotPriceCacheService;
    private final CandleRepository candleRepository;

    private final com.stock.analysis.service.HistoricalDataSyncService historicalDataSyncService;

    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusDto> getStatus() {
        return ResponseEntity.ok(webSocketStreamer.getCurrentStatus());
    }

    @GetMapping("/ticks/{symbol}")
    public ResponseEntity<Tick> getLatestTick(@PathVariable String symbol) {
        return spotPriceCacheService.getLatestTick(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/candles/{symbol}")
    public ResponseEntity<List<Candle>> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval) {
        String upperSymbol = symbol.toUpperCase();
        List<Candle> candles = candleRepository.findRecentCandles(upperSymbol, interval, PageRequest.of(0, 100));

        if (candles.isEmpty()) {
            candles = historicalDataSyncService.fetchAndStoreHistoricalCandles(upperSymbol, "NSE_EQ|" + upperSymbol, interval, java.time.LocalDate.now().minusDays(90), java.time.LocalDate.now());
        }

        return ResponseEntity.ok(candles);
    }
}
