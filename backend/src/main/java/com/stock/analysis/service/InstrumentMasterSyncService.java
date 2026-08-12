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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentMasterSyncService {

    private final InstrumentRepository instrumentRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (instrumentRepository.count() == 0) {
            log.info("Instrument Master DB table is empty. Initializing baseline instrument master records...");
            syncInstrumentMaster();
        }
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledDailySync() {
        log.info("Executing daily morning Instrument Master sync schedule...");
        syncInstrumentMaster();
    }

    public synchronized void syncInstrumentMaster() {
        try {
            List<Instrument> defaultInstruments = new ArrayList<>();

            defaultInstruments.add(Instrument.builder()
                    .instrumentKey("NSE_EQ|INE002A01018")
                    .symbol("RELIANCE")
                    .name("Reliance Industries Limited")
                    .exchange(Exchange.NSE_EQ)
                    .instrumentType(InstrumentType.EQUITY)
                    .lotSize(1)
                    .tickSize(0.05)
                    .build());

            defaultInstruments.add(Instrument.builder()
                    .instrumentKey("NSE_EQ|INE467B01029")
                    .symbol("TCS")
                    .name("Tata Consultancy Services Limited")
                    .exchange(Exchange.NSE_EQ)
                    .instrumentType(InstrumentType.EQUITY)
                    .lotSize(1)
                    .tickSize(0.05)
                    .build());

            defaultInstruments.add(Instrument.builder()
                    .instrumentKey("NSE_EQ|INE009A01021")
                    .symbol("INFY")
                    .name("Infosys Limited")
                    .exchange(Exchange.NSE_EQ)
                    .instrumentType(InstrumentType.EQUITY)
                    .lotSize(1)
                    .tickSize(0.05)
                    .build());

            defaultInstruments.add(Instrument.builder()
                    .instrumentKey("NSE_EQ|INE040A01034")
                    .symbol("HDFCBANK")
                    .name("HDFC Bank Limited")
                    .exchange(Exchange.NSE_EQ)
                    .instrumentType(InstrumentType.EQUITY)
                    .lotSize(1)
                    .tickSize(0.05)
                    .build());

            defaultInstruments.add(Instrument.builder()
                    .instrumentKey("NSE_INDEX|Nifty 50")
                    .symbol("NIFTY")
                    .name("NIFTY 50 Index")
                    .exchange(Exchange.NSE_EQ)
                    .instrumentType(InstrumentType.EQUITY)
                    .lotSize(50)
                    .tickSize(0.05)
                    .build());

            instrumentRepository.saveAll(defaultInstruments);
            log.info("Successfully synced {} instrument master records to database.", defaultInstruments.size());
        } catch (Exception e) {
            log.error("Failed to execute Instrument Master sync", e);
        }
    }
}
