package com.stock.analysis.service;

import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
import com.stock.analysis.model.Candle;
import com.stock.analysis.repository.CandleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataSyncService {

    private final CandleRepository candleRepository;
    private final UpstoxWebSocketStreamer webSocketStreamer;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upstox.api.base-url:https://api.upstox.com/v2}")
    private String baseUrl;

    @Transactional
    public List<Candle> fetchAndStoreHistoricalCandles(String symbol, String instrumentKey, String interval, LocalDate fromDate, LocalDate toDate) {
        List<Candle> savedCandles = new ArrayList<>();
        String accessToken = webSocketStreamer.getAccessToken();

        String upstoxInterval = mapToUpstoxInterval(interval);
        String dbInterval = mapToDbInterval(interval);

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String url = String.format("%s/historical-candle/%s/%s/%s/%s",
                        baseUrl, instrumentKey, upstoxInterval,
                        toDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candlesNode = root.path("data").path("candles");

                    if (candlesNode.isArray()) {
                        for (JsonNode cNode : candlesNode) {
                            String tsStr = cNode.get(0).asText();
                            Instant ts = Instant.parse(tsStr);

                            Candle candle = Candle.builder()
                                    .instrumentKey(instrumentKey)
                                    .symbol(symbol.toUpperCase())
                                    .intervalName(dbInterval)
                                    .timestamp(ts)
                                    .open(cNode.get(1).asDouble())
                                    .high(cNode.get(2).asDouble())
                                    .low(cNode.get(3).asDouble())
                                    .close(cNode.get(4).asDouble())
                                    .volume(cNode.get(5).asLong())
                                    .openInterest(cNode.size() > 6 ? cNode.get(6).asLong() : 0L)
                                    .build();

                            savedCandles.add(candle);
                        }
                        if (!savedCandles.isEmpty()) {
                            Instant minTs = savedCandles.stream().map(Candle::getTimestamp).min(Instant::compareTo).orElse(null);
                            Instant maxTs = savedCandles.stream().map(Candle::getTimestamp).max(Instant::compareTo).orElse(null);

                            if (minTs != null && maxTs != null) {
                                candleRepository.deleteBySymbolAndIntervalNameAndTimestampBetween(symbol.toUpperCase(), dbInterval, minTs, maxTs);
                            }

                            candleRepository.saveAll(savedCandles);
                            log.info("Fetched, deduplicated and saved {} historical candles for {}", savedCandles.size(), symbol);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch Upstox historical candles for symbol: " + symbol, e);
            }
        } else {
            log.warn("Cannot fetch Upstox historical candles for {}: Access token is missing.", symbol);
        }

        return savedCandles;
    }

    private String mapToUpstoxInterval(String interval) {
        if (interval == null) return "day";
        String lower = interval.toLowerCase();
        if (lower.equals("1d") || lower.equals("day") || lower.equals("daily")) return "day";
        if (lower.equals("1m") || lower.equals("1minute")) return "1minute";
        if (lower.equals("3m") || lower.equals("3minute")) return "3minute";
        if (lower.equals("5m") || lower.equals("5minute")) return "5minute";
        if (lower.equals("15m") || lower.equals("15minute")) return "15minute";
        if (lower.equals("30m") || lower.equals("30minute")) return "30minute";
        if (lower.equals("60m") || lower.equals("1h") || lower.equals("60minute")) return "60minute";
        if (lower.equals("1w") || lower.equals("week")) return "week";
        if (lower.equals("1mth") || lower.equals("month")) return "month";
        return interval;
    }

    private String mapToDbInterval(String interval) {
        if (interval == null) return "1d";
        String lower = interval.toLowerCase();
        if (lower.equals("day") || lower.equals("daily") || lower.equals("1d")) return "1d";
        if (lower.equals("1minute")) return "1m";
        if (lower.equals("3minute")) return "3m";
        if (lower.equals("5minute")) return "5m";
        if (lower.equals("15minute")) return "15m";
        if (lower.equals("30minute")) return "30m";
        if (lower.equals("60minute")) return "1h";
        if (lower.equals("week")) return "1w";
        if (lower.equals("month")) return "1M";
        return interval;
    }
}
