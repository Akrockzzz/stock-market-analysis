package com.stock.analysis.controller;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.ingestion.LruSubscriptionManager;
import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
import com.stock.analysis.model.Instrument;
import com.stock.analysis.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WatchlistController {

    private final InstrumentRepository instrumentRepository;
    private final LruSubscriptionManager subscriptionManager;
    private final UpstoxWebSocketStreamer webSocketStreamer;

    @PostMapping("/{symbol}/subscribe")
    public ResponseEntity<Map<String, Object>> subscribeSymbol(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();
        Instrument instrument = instrumentRepository.findBySymbolAndExchange(upperSymbol, Exchange.NSE_EQ)
                .orElseGet(() -> instrumentRepository.findBySymbolContainingIgnoreCase(upperSymbol).stream().findFirst()
                        .orElseThrow(() -> new MarketDataUnavailableException(upperSymbol, "Subscription", "Symbol not found in Instrument Master database.")));

        String key = instrument.getInstrumentKey();
        Set<String> evicted = subscriptionManager.subscribeTier1(List.of(key));

        webSocketStreamer.sendSubscriptions(Set.of(key), "sub");
        log.info("Subscribed symbol {} (Key: {}) to live stream", upperSymbol, key);

        return ResponseEntity.ok(Map.of(
                "symbol", upperSymbol,
                "instrumentKey", key,
                "status", "SUBSCRIBED",
                "evictedTokens", evicted
        ));
    }

    @DeleteMapping("/{symbol}/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribeSymbol(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();
        Instrument instrument = instrumentRepository.findBySymbolAndExchange(upperSymbol, Exchange.NSE_EQ)
                .orElseThrow(() -> new MarketDataUnavailableException(upperSymbol, "Unsubscribe", "Symbol not found in Instrument Master"));

        String key = instrument.getInstrumentKey();
        webSocketStreamer.sendSubscriptions(Set.of(key), "unsub");

        return ResponseEntity.ok(Map.of(
                "symbol", upperSymbol,
                "instrumentKey", key,
                "status", "UNSUBSCRIBED"
        ));
    }
}
