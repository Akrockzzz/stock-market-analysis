package com.stock.analysis.service;

import com.stock.analysis.model.Fundamentals;
import com.stock.analysis.repository.FundamentalsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalsSyncService {

    private final FundamentalsRepository fundamentalsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upstox.api.base-url:https://api.upstox.com/v2}")
    private String baseUrl;

    @Value("${upstox.api.access-token:}")
    private String accessToken;

    public Fundamentals fetchAndStoreFundamentals(String symbol, String instrumentKey) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Cannot fetch Upstox Fundamentals for {}: Access Token is missing.", symbol);
            return null;
        }

        try {
            String url = String.format("%s/market-quote/fundamentals?instrument_key=%s", baseUrl, instrumentKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                Fundamentals fundamentals = Fundamentals.builder()
                        .symbol(symbol.toUpperCase())
                        .period("TTM")
                        .revenueInCr(data.path("revenue").asDouble(0.0))
                        .netProfitInCr(data.path("net_profit").asDouble(0.0))
                        .ebitdaMarginPct(data.path("ebitda_margin").asDouble(0.0))
                        .netMarginPct(data.path("net_margin").asDouble(0.0))
                        .roePct(data.path("roe").asDouble(0.0))
                        .rocePct(data.path("roce").asDouble(0.0))
                        .debtToEquity(data.path("debt_to_equity").asDouble(0.0))
                        .peRatio(data.path("pe_ratio").asDouble(0.0))
                        .pbRatio(data.path("pb_ratio").asDouble(0.0))
                        .promoterHoldingPct(data.path("promoter_holding").asDouble(0.0))
                        .promoterPledgePct(data.path("promoter_pledge").asDouble(0.0))
                        .lastUpdated(Instant.now())
                        .build();

                return fundamentalsRepository.save(fundamentals);
            }
        } catch (Exception e) {
            log.error("Failed to fetch Upstox Fundamentals for " + symbol, e);
        }
        return null;
    }
}
