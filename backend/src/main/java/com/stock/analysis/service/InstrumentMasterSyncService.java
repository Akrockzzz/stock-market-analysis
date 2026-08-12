package com.stock.analysis.service;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import com.stock.analysis.model.Instrument;
import com.stock.analysis.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    private static final String UPSTOX_INSTRUMENT_GZ_URL = "https://assets.upstox.com/market-quote/instruments/exchange/complete.csv.gz";

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (instrumentRepository.count() == 0) {
            log.info("Instrument Master DB table is empty. Triggering full Upstox CSV download and sync...");
            syncInstrumentMaster();
        }
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledDailySync() {
        log.info("Executing scheduled morning Instrument Master sync...");
        syncInstrumentMaster();
    }

    public synchronized void syncInstrumentMaster() {
        List<Instrument> instruments = new ArrayList<>();
        log.info("Downloading Upstox Instrument Master CSV dump from: {}", UPSTOX_INSTRUMENT_GZ_URL);

        try (GZIPInputStream gzis = new GZIPInputStream(new URL(UPSTOX_INSTRUMENT_GZ_URL).openStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            String headerLine = reader.readLine(); // Header: instrument_key,exchange_token,tradingsymbol,name,last_price,expiry,strike,tick_size,lot_size,instrument_type,option_type,exchange
            if (headerLine == null) return;

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 10) continue;

                String instrumentKey = tokens[0].replaceAll("\"", "");
                String exchangeStr = tokens[11 < tokens.length ? 11 : 1].replaceAll("\"", "");
                String symbol = tokens[2].replaceAll("\"", "");
                String name = tokens[3].replaceAll("\"", "");
                String expiryStr = tokens[5].replaceAll("\"", "");
                String strikeStr = tokens[6].replaceAll("\"", "");
                String tickSizeStr = tokens[7].replaceAll("\"", "");
                String lotSizeStr = tokens[8].replaceAll("\"", "");
                String typeStr = tokens[9].replaceAll("\"", "");

                if (!"NSE_EQ".equalsIgnoreCase(exchangeStr) && !"NSE_FO".equalsIgnoreCase(exchangeStr)) {
                    continue; // Filter to NSE Equity and F&O
                }

                InstrumentType type = InstrumentType.EQUITY;
                if ("FUT".equalsIgnoreCase(typeStr) || "FUTIVX".equalsIgnoreCase(typeStr)) type = InstrumentType.FUTURES;
                else if ("CE".equalsIgnoreCase(typeStr)) type = InstrumentType.CE;
                else if ("PE".equalsIgnoreCase(typeStr)) type = InstrumentType.PE;

                Double strike = !strikeStr.isBlank() ? parseDouble(strikeStr) : null;
                Double tickSize = !tickSizeStr.isBlank() ? parseDouble(tickSizeStr) : 0.05;
                Integer lotSize = !lotSizeStr.isBlank() ? parseInt(lotSizeStr) : 1;
                LocalDate expiry = !expiryStr.isBlank() ? parseExpiry(expiryStr) : null;

                Instrument instrument = Instrument.builder()
                        .instrumentKey(instrumentKey)
                        .exchange("NSE_EQ".equalsIgnoreCase(exchangeStr) ? Exchange.NSE_EQ : Exchange.NSE_FO)
                        .symbol(symbol)
                        .name(name)
                        .instrumentType(type)
                        .lotSize(lotSize)
                        .strikePrice(strike)
                        .expiry(expiry)
                        .tickSize(tickSize)
                        .build();

                instruments.add(instrument);
                count++;

                if (instruments.size() >= 2000) {
                    instrumentRepository.saveAll(instruments);
                    instruments.clear();
                }
            }

            if (!instruments.isEmpty()) {
                instrumentRepository.saveAll(instruments);
            }
            log.info("Successfully downloaded and parsed {} NSE instruments into database.", count);
        } catch (Exception e) {
            log.error("Failed to download or parse Upstox Instrument Master CSV dump. Falling back to seed instruments.", e);
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

    private Double parseDouble(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return null; }
    }

    private Integer parseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 1; }
    }

    private LocalDate parseExpiry(String val) {
        try { return LocalDate.parse(val, DateTimeFormatter.ISO_LOCAL_DATE); } catch (Exception e) { return null; }
    }
}
