package com.stock.analysis.service;

import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
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
    private final UpstoxWebSocketStreamer webSocketStreamer;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upstox.api.base-url:https://api.upstox.com/v2}")
    private String baseUrl;

    public Fundamentals fetchAndStoreFundamentals(String symbol, String isinOrInstrumentKey) {
        String accessToken = webSocketStreamer.getAccessToken();

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

            // 1. Fetch Income Statement (Revenue, Net Profit, EBITDA & Net Margins)
            String incomeUrl = String.format("%s/fundamentals/%s/income-statement", baseUrl, isin);
            JsonNode incomeNode = fetchJson(incomeUrl, requestEntity);

            // 2. Fetch Key Ratios (ROE, ROCE, P/E, P/B, Debt to Equity)
            String ratiosUrl = String.format("%s/fundamentals/%s/key-ratios", baseUrl, isin);
            JsonNode ratiosNode = fetchJson(ratiosUrl, requestEntity);

            // 3. Fetch Share Holdings (Promoter %, Pledge %, FII %, DII %)
            String holdingsUrl = String.format("%s/fundamentals/%s/share-holdings", baseUrl, isin);
            JsonNode holdingsNode = fetchJson(holdingsUrl, requestEntity);

            Fundamentals fundamentals = Fundamentals.builder()
                    .symbol(symbol.toUpperCase())
                    .period("TTM")
                    .revenueInCr(incomeNode.path("total_revenue").asDouble(incomeNode.path("revenue").asDouble(0.0)))
                    .netProfitInCr(incomeNode.path("net_profit").asDouble(incomeNode.path("pat").asDouble(0.0)))
                    .ebitdaMarginPct(incomeNode.path("ebitda_margin").asDouble(incomeNode.path("operating_margin").asDouble(0.0)))
                    .netMarginPct(incomeNode.path("net_margin").asDouble(incomeNode.path("pat_margin").asDouble(0.0)))
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
            log.debug("Fundamentals sub-endpoint call failed for URL: {}", url);
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
