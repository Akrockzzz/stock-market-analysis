package com.stock.analysis.controller;

import com.stock.analysis.ingestion.UpstoxWebSocketStreamer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/upstox")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UpstoxTokenController {

    private final UpstoxWebSocketStreamer webSocketStreamer;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> updateAccessToken(@RequestBody TokenUpdateRequest request) {
        if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Access token cannot be empty"));
        }

        log.info("Received runtime Upstox OAuth token update. Reconnecting market stream...");
        webSocketStreamer.updateAccessToken(request.getAccessToken());

        return ResponseEntity.ok(Map.of(
                "status", "TOKEN_UPDATED",
                "message", "Upstox access token updated successfully. Market stream reconnect triggered."
        ));
    }

    @Data
    public static class TokenUpdateRequest {
        private String accessToken;
    }
}
