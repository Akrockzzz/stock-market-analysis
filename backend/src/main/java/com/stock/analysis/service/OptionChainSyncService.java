package com.stock.analysis.service;

import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.engine.OptionChainAnalysisService;
import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
import com.stock.analysis.model.OptionChainSnapshot;
import com.stock.analysis.repository.OptionChainSnapshotRepository;
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
public class OptionChainSyncService {

    private final OptionChainSnapshotRepository optionChainSnapshotRepository;
    private final OptionChainAnalysisService optionChainAnalysisService;
    private final UpstoxWebSocketStreamer webSocketStreamer;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upstox.api.base-url:https://api.upstox.com/v2}")
    private String baseUrl;

    public OptionChainSnapshot fetchAndStoreOptionChain(String underlyingSymbol, String instrumentKey, LocalDate expiryDate, Double spotPrice) {
        OptionChainSnapshot snapshot = null;
        String accessToken = webSocketStreamer.getAccessToken();

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String url = String.format("%s/option/chain?instrument_key=%s&expiry_date=%s",
                        baseUrl, instrumentKey, expiryDate.toString());

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode dataArray = root.path("data");

                    List<StrikeDataDto> strikes = new ArrayList<>();
                    if (dataArray.isArray()) {
                        for (JsonNode sNode : dataArray) {
                            double strikePrice = sNode.path("strike_price").asDouble();
                            JsonNode callData = sNode.path("call_options");
                            JsonNode putData = sNode.path("put_options");

                            long callOi = callData.path("market_data").path("oi").asLong(0);
                            long putOi = putData.path("market_data").path("oi").asLong(0);
                            double callLtp = callData.path("market_data").path("ltp").asDouble(0.0);
                            double putLtp = putData.path("market_data").path("ltp").asDouble(0.0);
                            double callIv = callData.path("option_greeks").path("iv").asDouble(0.0);
                            double putIv = putData.path("option_greeks").path("iv").asDouble(0.0);

                            strikes.add(StrikeDataDto.builder()
                                    .strikePrice(strikePrice)
                                    .callOi(callOi)
                                    .putOi(putOi)
                                    .callLtp(callLtp)
                                    .putLtp(putLtp)
                                    .callIv(callIv)
                                    .putIv(putIv)
                                    .build());
                        }
                    }

                    if (!strikes.isEmpty() && spotPrice != null && spotPrice > 0) {
                        var analysis = optionChainAnalysisService.analyzeOptionChain(underlyingSymbol, spotPrice, strikes);

                        snapshot = OptionChainSnapshot.builder()
                                .underlyingSymbol(underlyingSymbol.toUpperCase())
                                .expiryDate(expiryDate)
                                .spotPrice(spotPrice)
                                .atmStrike(analysis.getAtmStrike())
                                .putCallRatio(analysis.getPutCallRatio())
                                .maxPainStrike(analysis.getMaxPainStrike())
                                .jsonStrikeData(objectMapper.writeValueAsString(strikes))
                                .timestamp(Instant.now())
                                .build();

                        snapshot = optionChainSnapshotRepository.save(snapshot);
                        return snapshot;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch Upstox Option Chain for " + underlyingSymbol, e);
            }
        }

        return snapshot;
    }
}
