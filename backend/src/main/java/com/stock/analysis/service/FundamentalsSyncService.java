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

    public Fundamentals fetchAndStoreFundamentals(String symbol, String isinOrInstrumentKey) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Cannot fetch Upstox Fundamentals for {}: Access Token is missing.", symbol);
            return null;
        }

        String isin = extractIsin(isinOrInstrumentKey);
        if (isin == null || isin.isBlank()) {
            log.warn("Cannot fetch Upstox Fundamentals for {}: Valid ISIN code missing.", symbol);
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 1. Fetch Key Ratios
            String ratiosUrl = String.format("%s/fundamentals/%s/key-ratios", baseUrl, isin);
            JsonNode ratiosNode = fetchJson(ratiosUrl, requestEntity);

            // 2. Fetch Share Holdings
            String holdingsUrl = String.format("%s/fundamentals/%s/share-holdings", baseUrl, isin);
            JsonNode holdingsNode = fetchJson(holdingsUrl, requestEntity);

            // 3. Fetch Profile / Financial Statements
            String profileUrl = String.format("%s/fundamentals/%s/profile", baseUrl, isin);
            JsonNode profileNode = fetchJson(profileUrl, requestEntity);

            Fundamentals fundamentals = Fundamentals.builder()
                    .symbol(symbol.toUpperCase())
                    .period("TTM")
                    .revenueInCr(profileNode.path("revenue").asDouble(0.0))
                    .netProfitInCr(profileNode.path("net_profit").asDouble(0.0))
                    .ebitdaMarginPct(profileNode.path("ebitda_margin").asDouble(0.0))
                    .netMarginPct(profileNode.path("net_margin").asDouble(0.0))
                    .roePct(ratiosNode.path("roe").asDouble(ratiosNode.path("return_on_equity").asDouble(0.0)))
                    .rocePct(ratiosNode.path("roce").asDouble(ratiosNode.path("return_on_capital_employed").asDouble(0.0)))
                    .debtToEquity(ratiosNode.path("debt_to_equity").asDouble(0.0))
                    .peRatio(ratiosNode.path("pe_ratio").asDouble(0.0))
                    .pbRatio(ratiosNode.path("pb_ratio").asDouble(0.0))
                    .evEbitda(ratiosNode.path("ev_ebitda").asDouble(0.0))
                    .promoterHoldingPct(holdingsNode.path("promoter_holding").asDouble(0.0))
                    .promoterPledgePct(holdingsNode.path("promoter_pledge").asDouble(0.0))
                    .fiiHoldingPct(holdingsNode.path("fii_holding").asDouble(0.0))
                    .diiHoldingPct(holdingsNode.path("dii_holding").asDouble(0.0))
                    .lastUpdated(Instant.now())
                    .build();

            return fundamentalsRepository.save(fundamentals);
        } catch (Exception e) {
            log.error("Failed to fetch Upstox Fundamentals by ISIN for " + symbol, e);
        }
        return null;
    }

    private JsonNode fetchJson(String url, HttpEntity<Void> requestEntity) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody()).path("data");
            }
        } catch (Exception e) {
            log.debug("Sub-endpoint call failed for URL: {}", url);
        }
        return objectMapper.createObjectNode();
    }

    private String extractIsin(String input) {
        if (input == null) return null;
        if (input.startsWith("INE") || input.startsWith("IN0")) return input;
        int pipeIdx = input.indexOf('|');
        if (pipeIdx >= 0) {
            String candidate = input.substring(pipeIdx + 1);
            if (candidate.startsWith("INE") || candidate.startsWith("IN0")) return candidate;
        }
        return input;
    }
}
