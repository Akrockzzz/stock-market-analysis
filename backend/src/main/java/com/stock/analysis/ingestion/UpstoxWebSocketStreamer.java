package com.stock.analysis.ingestion;

import com.stock.analysis.dto.ConnectionStatusDto;
import com.stock.analysis.enums.ConnectionState;
import com.stock.analysis.model.Tick;
import com.stock.analysis.service.SpotPriceCacheService;
import com.stock.analysis.util.MarketHoursUtil;
import com.upstox.marketdata.v3.MarketDataFeedV3;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class UpstoxWebSocketStreamer {

    private final String wsFeedUrl;
    private final AtomicReference<String> accessToken;
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
        this.accessToken = new AtomicReference<>(accessToken != null ? accessToken : "");
        this.spotPriceCacheService = spotPriceCacheService;
        this.marketHoursUtil = marketHoursUtil;
        this.messagingTemplate = messagingTemplate;
        this.subscriptionManager = subscriptionManager;
    }

    public synchronized void updateAccessToken(String newToken) {
        if (newToken == null || newToken.isBlank()) return;
        log.info("Updating dynamic Upstox Access Token reference and reconnecting stream...");
        this.accessToken.set(newToken);
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                log.warn("Error closing previous WebSocket client during token update", e);
            }
        }
        connect();
    }

    public String getAccessToken() {
        return accessToken.get();
    }

    public synchronized void connect() {
        String token = accessToken.get();
        if (token == null || token.isBlank()) {
            log.warn("Upstox Access Token is missing. Connection state: NOT_CONNECTED");
            broadcastStatus(ConnectionState.NOT_CONNECTED, "Upstox Access Token missing. Configure in settings.");
            return;
        }

        try {
            URI uri = new URI(wsFeedUrl);
            Map<String, String> headers = ConcurrentHashMap.newKeySet().stream()
                    .collect(ConcurrentHashMap::new, (m, v) -> {}, (m1, m2) -> {});
            headers.put("Authorization", "Bearer " + token);

            webSocketClient = new WebSocketClient(uri, headers) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    log.info("Successfully connected to Upstox Market Data Feed V3 WebSocket");
                    ConnectionState state = marketHoursUtil.determineSystemConnectionState(true, true);
                    broadcastStatus(state, "Stream connected successfully");

                    // Subscribe to active LRU tokens
                    sendSubscriptions(subscriptionManager.getActiveTier1Tokens(), "sub");
                }

                @Override
                public void onMessage(String message) {
                    log.debug("Received text message from Upstox feed: {}", message);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
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

    public void sendSubscriptions(Set<String> keys, String action) {
        if (!isConnected || webSocketClient == null || keys.isEmpty()) return;
        try {
            String jsonKeys = String.join("\",\"", keys);
            String subJson = String.format("{\"guid\":\"sub_req\",\"method\":\"%s\",\"data\":{\"mode\":\"full\",\"instrumentKeys\":[\"%s\"]}}", action, jsonKeys);
            webSocketClient.send(subJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("Sent binary WebSocket subscription frame action '{}' for keys: {}", action, keys);
        } catch (Exception e) {
            log.error("Error sending WebSocket subscription frame", e);
        }
    }

    public ConnectionStatusDto getCurrentStatus() {
        String token = accessToken.get();
        boolean hasToken = token != null && !token.isBlank();
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
        try {
            byte[] byteArray = new byte[bytes.remaining()];
            bytes.get(byteArray);
            MarketDataFeedV3.FeedResponse feedResponse = MarketDataFeedV3.FeedResponse.parseFrom(byteArray);

            if (feedResponse != null && feedResponse.getFeedsCount() > 0) {
                feedResponse.getFeedsMap().forEach((instrumentKey, feed) -> {
                    Double ltp = null;
                    Long volume = null;
                    Long oi = null;

                    if (feed.hasLtpc()) {
                        ltp = feed.getLtpc().getLtp();
                    } else if (feed.hasFullFeed()) {
                        MarketDataFeedV3.FullFeed ff = feed.getFullFeed();
                        if (ff.hasLtpc()) ltp = ff.getLtpc().getLtp();
                        volume = ff.getVolume();
                        oi = ff.getOi();
                    }

                    if (ltp != null && ltp > 0) {
                        Tick tick = Tick.builder()
                                .instrumentKey(instrumentKey)
                                .symbol(extractSymbolFromKey(instrumentKey))
                                .ltp(ltp)
                                .volume(volume)
                                .openInterest(oi)
                                .timestamp(Instant.ofEpochMilli(feedResponse.getCurrentTs() > 0 ? feedResponse.getCurrentTs() : System.currentTimeMillis()))
                                .build();

                        subscriptionManager.updateAccess(instrumentKey);
                        spotPriceCacheService.updateTick(tick);
                        messagingTemplate.convertAndSend("/topic/ticks", tick);
                    }
                });
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Error unmarshalling Protobuf payload from Upstox stream", e);
        } catch (Exception e) {
            log.error("Unexpected error processing binary stream payload", e);
        }
    }

    private String extractSymbolFromKey(String instrumentKey) {
        if (instrumentKey == null) return "UNKNOWN";
        int barIdx = instrumentKey.indexOf('|');
        return barIdx >= 0 ? instrumentKey.substring(barIdx + 1) : instrumentKey;
    }
}
