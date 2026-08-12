package com.stock.analysis.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LruSubscriptionManager {

    private final int tier1MaxTokens;
    private final int tier2MaxTokens;

    // LRU Maps: token -> lastAccessedTimestamp
    private final Map<String, Long> tier1Subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Long> tier2Subscriptions = new ConcurrentHashMap<>();

    public LruSubscriptionManager(
            @Value("${upstox.subscription.tier1-max-tokens:2500}") int tier1MaxTokens,
            @Value("${upstox.subscription.tier2-max-tokens:500}") int tier2MaxTokens) {
        this.tier1MaxTokens = tier1MaxTokens;
        this.tier2MaxTokens = tier2MaxTokens;
    }

    public synchronized Set<String> subscribeTier1(List<String> instrumentKeys) {
        Set<String> evictedTokens = new HashSet<>();
        long now = System.currentTimeMillis();

        for (String key : instrumentKeys) {
            if (!tier1Subscriptions.containsKey(key) && tier1Subscriptions.size() >= tier1MaxTokens) {
                String lruKey = findLruToken(tier1Subscriptions);
                if (lruKey != null) {
                    tier1Subscriptions.remove(lruKey);
                    evictedTokens.add(lruKey);
                    log.info("LRU Evicted Tier 1 token: {}", lruKey);
                }
            }
            tier1Subscriptions.put(key, now);
        }
        return evictedTokens;
    }

    public synchronized Set<String> subscribeTier2(List<String> instrumentKeys) {
        Set<String> evictedTokens = new HashSet<>();
        long now = System.currentTimeMillis();

        for (String key : instrumentKeys) {
            if (!tier2Subscriptions.containsKey(key) && tier2Subscriptions.size() >= tier2MaxTokens) {
                String lruKey = findLruToken(tier2Subscriptions);
                if (lruKey != null) {
                    tier2Subscriptions.remove(lruKey);
                    evictedTokens.add(lruKey);
                    log.info("LRU Evicted Tier 2 token: {}", lruKey);
                }
            }
            tier2Subscriptions.put(key, now);
        }
        return evictedTokens;
    }

    public void updateAccess(String instrumentKey) {
        long now = System.currentTimeMillis();
        if (tier1Subscriptions.containsKey(instrumentKey)) {
            tier1Subscriptions.put(instrumentKey, now);
        }
        if (tier2Subscriptions.containsKey(instrumentKey)) {
            tier2Subscriptions.put(instrumentKey, now);
        }
    }

    public Set<String> getActiveTier1Tokens() {
        return new HashSet<>(tier1Subscriptions.keySet());
    }

    public Set<String> getActiveTier2Tokens() {
        return new HashSet<>(tier2Subscriptions.keySet());
    }

    private String findLruToken(Map<String, Long> subMap) {
        return subMap.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
