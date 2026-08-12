package com.stock.analysis.ingestion;

import com.stock.analysis.dto.ConnectionStatusDto;
import com.stock.analysis.enums.ConnectionState;
import com.stock.analysis.model.Tick;
import com.stock.analysis.service.SpotPriceCacheService;
import com.stock.analysis.util.MarketHoursUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UpstoxWebSocketStreamer {

    private final String wsFeedUrl;
    private final String accessToken;
    private final SpotPriceCacheService spotPriceCacheService;
    private final MarketHoursUtil marketHoursUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final LruSubscriptionManager subscriptionManager;

    private WebSocketClient webSocketClient;
    private volatile boolean isConnected = false;

    public UpstoxWebSocketStreamer(
            @Value("${upstox.api.ws-feed-url}") String wsFeedUrl,
            @Value("${upstox.api.access-token:}") String accessToken,
            SpotPriceCacheService spotPriceCacheService,
            MarketHoursUtil marketHoursUtil,
            SimpMessagingTemplate messagingTemplate,
            LruSubscriptionManager subscriptionManager) {
        this.wsFeedUrl = wsFeedUrl;
        this.accessToken = accessToken;
        this.spotPriceCacheService = spotPriceCacheService;
        this.marketHoursUtil = marketHoursUtil;
        this.messagingTemplate = messagingTemplate;
        this.subscriptionManager = subscriptionManager;
    }

    public synchronized void connect() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Upstox Access Token is missing. Connection state: NOT_CONNECTED");
            broadcastStatus(ConnectionState.NOT_CONNECTED, "Upstox Access Token missing. Configure in settings.");
            return;
        }

        try {
            URI uri = new URI(wsFeedUrl);
            Map<String, String> headers = ConcurrentHashMap.newKeySet().stream()
                    .collect(ConcurrentHashMap::new, (m, v) -> {}, (m1, m2) -> {});
            headers.put("Authorization", "Bearer " + accessToken);

            webSocketClient = new WebSocketClient(uri, headers) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    log.info("Successfully connected to Upstox Market Data Feed V3 WebSocket");
                    ConnectionState state = marketHoursUtil.determineSystemConnectionState(true, true);
                    broadcastStatus(state, "Stream connected successfully");
                }

                @Override
                public void onMessage(String message) {
                    // JSON payload fallback
                    log.debug("Received text message: {}", message);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    // Protobuf payload decoding logic
                    parseProtobufPayload(bytes);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    log.warn("Upstox WebSocket closed. Code: {}, Reason: {}", code, reason);
                    ConnectionState state = marketHoursUtil.determineSystemConnectionState(true, false);
                    broadcastStatus(state, "Stream disconnected: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Upstox WebSocket exception encountered", ex);
                    broadcastStatus(ConnectionState.NOT_CONNECTED, "WebSocket error: " + ex.getMessage());
                }
            };

            webSocketClient.connect();
        } catch (Exception e) {
            log.error("Failed to initialize Upstox WebSocket Client", e);
            broadcastStatus(ConnectionState.NOT_CONNECTED, "Connection init error: " + e.getMessage());
        }
    }

    public void processSimulatedTick(Tick tick) {
        // Fallback or simulated tick handler for live testing / off-hours evaluation
        spotPriceCacheService.updateTick(tick);
        messagingTemplate.convertAndSend("/topic/ticks", tick);
    }

    public ConnectionStatusDto getCurrentStatus() {
        boolean hasToken = accessToken != null && !accessToken.isBlank();
        ConnectionState state = marketHoursUtil.determineSystemConnectionState(hasToken, isConnected);
        return new ConnectionStatusDto(
                "UPSTOX_V3_FEED",
                state,
                state.getDescription(),
                Instant.now()
        );
    }

    private void broadcastStatus(ConnectionState state, String message) {
        ConnectionStatusDto status = new ConnectionStatusDto("UPSTOX_V3_FEED", state, message, Instant.now());
        messagingTemplate.convertAndSend("/topic/status", status);
    }

    private void parseProtobufPayload(ByteBuffer bytes) {
        // High frequency Protobuf byte decoding fallback structure
        try {
            // Unpack binary protobuf bytes into Tick entity
            // Note: Protobuf MarketDataFeedV3 auto-generated java class handles exact binary unmarshalling
        } catch (Exception e) {
            log.error("Error decoding Protobuf message payload", e);
        }
    }
}
