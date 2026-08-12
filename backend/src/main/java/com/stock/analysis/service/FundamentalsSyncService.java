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
                    .revenueInCr(extractFieldDouble(incomeNode, "total_revenue", "revenue", "sales", "total_sales"))
                    .netProfitInCr(extractFieldDouble(incomeNode, "net_profit", "pat", "profit_after_tax"))
                    .ebitdaMarginPct(extractFieldDouble(incomeNode, "ebitda_margin", "operating_margin", "opm"))
                    .netMarginPct(extractFieldDouble(incomeNode, "net_margin", "pat_margin", "npm"))
                    .roePct(extractFieldDouble(ratiosNode, "roe", "return_on_equity", "roe_pct"))
                    .rocePct(extractFieldDouble(ratiosNode, "roce", "return_on_capital_employed", "roce_pct"))
                    .debtToEquity(extractFieldDouble(ratiosNode, "debt_to_equity", "d_e", "de_ratio"))
                    .peRatio(extractFieldDouble(ratiosNode, "pe_ratio", "pe", "price_to_earnings"))
                    .pbRatio(extractFieldDouble(ratiosNode, "pb_ratio", "pb", "price_to_book"))
                    .evEbitda(extractFieldDouble(ratiosNode, "ev_ebitda", "ev_to_ebitda"))
                    .promoterHoldingPct(extractFieldDouble(holdingsNode, "promoter_holding", "promoter", "promoters"))
                    .promoterPledgePct(extractFieldDouble(holdingsNode, "promoter_pledge", "pledged_shares", "pledge_pct"))
                    .fiiHoldingPct(extractFieldDouble(holdingsNode, "fii_holding", "fii", "fiis"))
                    .diiHoldingPct(extractFieldDouble(holdingsNode, "dii_holding", "dii", "diis"))
                    .lastUpdated(Instant.now())
                    .build();

            return fundamentalsRepository.save(fundamentals);
        } catch (Exception e) {
            log.error("Failed to fetch Upstox Fundamentals by ISIN for " + symbol, e);
        }
        return null;
    }

    private double extractFieldDouble(JsonNode root, String... keys) {
        if (root == null || root.isMissingNode()) return 0.0;
        for (String key : keys) {
            if (root.has(key) && !root.path(key).isNull()) {
                return root.path(key).asDouble(0.0);
            }
        }
        return 0.0;
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
