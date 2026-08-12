package com.stock.analysis.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.stock.analysis.model.Tick;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class SpotPriceCacheService {

    // Caffeine cache holding key: symbol/instrumentKey -> Tick
    private final Cache<String, Tick> tickCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(5000)
            .build();

    public void updateTick(Tick tick) {
        if (tick != null && tick.getInstrumentKey() != null) {
            tickCache.put(tick.getInstrumentKey(), tick);
            if (tick.getSymbol() != null) {
                tickCache.put(tick.getSymbol(), tick);
            }
        }
    }

    public Optional<Tick> getLatestTick(String symbolOrKey) {
        return Optional.ofNullable(tickCache.getIfPresent(symbolOrKey));
    }
}
