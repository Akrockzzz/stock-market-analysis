package com.stock.analysis.service;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import com.stock.analysis.model.Instrument;
import com.stock.analysis.repository.InstrumentRepository;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentMasterSyncService {

    private final InstrumentRepository instrumentRepository;

    private static final String UPSTOX_INSTRUMENT_JSON_GZ_URL = "https://assets.upstox.com/market-quote/instruments/exchange/complete.json.gz";

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (instrumentRepository.count() == 0) {
            log.info("Instrument Master DB table is empty. Triggering full Upstox JSON dump sync...");
            syncInstrumentMaster();
        }
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledDailySync() {
        log.info("Executing scheduled morning Instrument Master sync...");
        syncInstrumentMaster();
    }

    public synchronized void syncInstrumentMaster() {
        List<Instrument> batch = new ArrayList<>();
        log.info("Downloading official Upstox Instrument Master JSON dump from: {}", UPSTOX_INSTRUMENT_JSON_GZ_URL);

        try (InputStream stream = new URL(UPSTOX_INSTRUMENT_JSON_GZ_URL).openStream();
             GZIPInputStream gzis = new GZIPInputStream(stream)) {

            JsonFactory factory = new JsonFactory();
            try (JsonParser parser = factory.createParser(gzis)) {

                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    log.warn("Expected START_ARRAY token in Upstox Instrument JSON dump");
                    seedFallbackInstruments();
                    return;
                }

                int count = 0;
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    String instrumentKey = null;
                    String exchangeStr = null;
                    String symbol = null;
                    String name = null;
                    String isin = null;
                    String expiryStr = null;
                    Double strikePrice = null;
                    Double tickSize = 0.05;
                    Integer lotSize = 1;
                    String instrumentTypeStr = null;

                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String fieldName = parser.getCurrentName();
                        parser.nextToken(); // Move to field value

                        if ("instrument_key".equals(fieldName)) instrumentKey = parser.getText();
                        else if ("exchange".equals(fieldName)) exchangeStr = parser.getText();
                        else if ("trading_symbol".equals(fieldName) || "symbol".equals(fieldName)) symbol = parser.getText();
                        else if ("name".equals(fieldName)) name = parser.getText();
                        else if ("isin".equals(fieldName)) isin = parser.getText();
                        else if ("expiry".equals(fieldName)) expiryStr = parser.getText();
                        else if ("strike_price".equals(fieldName) || "strike".equals(fieldName)) strikePrice = parser.getValueAsDouble();
                        else if ("tick_size".equals(fieldName)) tickSize = parser.getValueAsDouble(0.05);
                        else if ("lot_size".equals(fieldName)) lotSize = parser.getValueAsInt(1);
                        else if ("instrument_type".equals(fieldName)) instrumentTypeStr = parser.getText();
                    }

                    if (!"NSE_EQ".equalsIgnoreCase(exchangeStr) && !"NSE_FO".equalsIgnoreCase(exchangeStr)) {
                        continue; // Filter to NSE Equity and F&O instruments
                    }

                    if (instrumentKey != null && symbol != null) {
                        InstrumentType type = InstrumentType.EQUITY;
                        if ("FUT".equalsIgnoreCase(instrumentTypeStr) || "FUTIVX".equalsIgnoreCase(instrumentTypeStr)) type = InstrumentType.FUTURES;
                        else if ("CE".equalsIgnoreCase(instrumentTypeStr)) type = InstrumentType.CE;
                        else if ("PE".equalsIgnoreCase(instrumentTypeStr)) type = InstrumentType.PE;

                        LocalDate expiry = (expiryStr != null && !expiryStr.isBlank()) ? parseExpiry(expiryStr) : null;

                        Instrument instrument = Instrument.builder()
                                .instrumentKey(instrumentKey)
                                .exchange("NSE_EQ".equalsIgnoreCase(exchangeStr) ? Exchange.NSE_EQ : Exchange.NSE_FO)
                                .symbol(symbol)
                                .name(name)
                                .instrumentType(type)
                                .lotSize(lotSize)
                                .strikePrice(strikePrice)
                                .expiry(expiry)
                                .tickSize(tickSize)
                                .build();

                        batch.add(instrument);
                        count++;

                        if (batch.size() >= 2000) {
                            instrumentRepository.saveAll(batch);
                            batch.clear();
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    instrumentRepository.saveAll(batch);
                }
                log.info("Successfully downloaded and parsed {} NSE instruments from Upstox JSON dump.", count);
            }
        } catch (Exception e) {
            log.error("Failed to download or parse Upstox Instrument Master JSON dump. Falling back to baseline instruments.", e);
            seedFallbackInstruments();
        }
    }

    private void seedFallbackInstruments() {
        List<Instrument> seeds = List.of(
                Instrument.builder().instrumentKey("NSE_EQ|INE002A01018").symbol("RELIANCE").name("Reliance Industries Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).lotSize(1).tickSize(0.05).build(),
                Instrument.builder().instrumentKey("NSE_EQ|INE467B01029").symbol("TCS").name("Tata Consultancy Services Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).lotSize(1).tickSize(0.05).build(),
                Instrument.builder().instrumentKey("NSE_EQ|INE009A01021").symbol("INFY").name("Infosys Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).lotSize(1).tickSize(0.05).build(),
                Instrument.builder().instrumentKey("NSE_EQ|INE040A01034").symbol("HDFCBANK").name("HDFC Bank Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).lotSize(1).tickSize(0.05).build(),
                Instrument.builder().instrumentKey("NSE_INDEX|Nifty 50").symbol("NIFTY").name("NIFTY 50 Index").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).lotSize(50).tickSize(0.05).build()
        );
        instrumentRepository.saveAll(seeds);
    }

    private LocalDate parseExpiry(String val) {
        try { return LocalDate.parse(val, DateTimeFormatter.ISO_LOCAL_DATE); } catch (Exception e) { return null; }
    }
}
