package com.stock.analysis.service;

import com.stock.analysis.model.Candle;
import com.stock.analysis.repository.CandleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataSyncService {

    private final CandleRepository candleRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upstox.api.v3-base-url:https://api.upstox.com/v2}")
    private String baseUrl;

    @Value("${upstox.api.access-token:}")
    private String accessToken;

    public List<Candle> fetchAndStoreHistoricalCandles(String symbol, String instrumentKey, String interval, LocalDate fromDate, LocalDate toDate) {
        List<Candle> savedCandles = new ArrayList<>();

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Cannot fetch Upstox historical candles for {}: Upstox Access Token is missing.", symbol);
            return savedCandles;
        }

        try {
            String url = String.format("%s/historical-candle/%s/%s/%s/%s",
                    baseUrl, instrumentKey, interval, toDate.toString(), fromDate.toString());

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
                        // Upstox candle array format: [timestamp, open, high, low, close, volume, openInterest]
                        String tsStr = cNode.get(0).asText();
                        Instant ts = Instant.parse(tsStr);

                        Candle candle = Candle.builder()
                                .instrumentKey(instrumentKey)
                                .symbol(symbol.toUpperCase())
                                .intervalName(interval)
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
                        candleRepository.saveAll(savedCandles);
                        log.info("Successfully fetched and saved {} real historical candles for {}", savedCandles.size(), symbol);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch historical candles for symbol: " + symbol, e);
        }
        return savedCandles;
    }
}
