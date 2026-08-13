package com.stock.analysis.controller;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import com.stock.analysis.model.Instrument;
import com.stock.analysis.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/instruments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentRepository instrumentRepository;

    private static final List<Instrument> POPULAR_INDIAN_STOCKS = List.of(
            Instrument.builder().instrumentKey("NSE_EQ|INE002A01018").symbol("RELIANCE").name("Reliance Industries Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE467B01029").symbol("TCS").name("Tata Consultancy Services Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE009A01021").symbol("INFY").name("Infosys Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE040A01034").symbol("HDFCBANK").name("HDFC Bank Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE090A01021").symbol("ICICIBANK").name("ICICI Bank Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE062A01020").symbol("SBIN").name("State Bank of India").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE155A01022").symbol("TATAMOTORS").name("Tata Motors Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE397D01024").symbol("BHARTIARTL").name("Bharti Airtel Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE154A01025").symbol("ITC").name("ITC Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_EQ|INE018A01030").symbol("L&T").name("Larsen & Toubro Limited").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_INDEX|Nifty 50").symbol("NIFTY").name("NIFTY 50 Index").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build(),
            Instrument.builder().instrumentKey("NSE_INDEX|Nifty Bank").symbol("BANKNIFTY").name("NIFTY Bank Index").exchange(Exchange.NSE_EQ).instrumentType(InstrumentType.EQUITY).build()
    );

    @GetMapping("/search")
    public ResponseEntity<List<Instrument>> searchInstruments(@RequestParam(required = false, defaultValue = "") String query) {
        String q = query.trim().toUpperCase();

        List<Instrument> dbResults = instrumentRepository.findBySymbolContainingIgnoreCase(q);

        if (dbResults.isEmpty()) {
            dbResults = instrumentRepository.findAll().stream()
                    .filter(i -> (i.getSymbol() != null && i.getSymbol().toUpperCase().contains(q)) ||
                            (i.getName() != null && i.getName().toUpperCase().contains(q)))
                    .limit(20)
                    .collect(Collectors.toList());
        }

        if (dbResults.isEmpty()) {
            dbResults = POPULAR_INDIAN_STOCKS.stream()
                    .filter(i -> q.isEmpty() || i.getSymbol().contains(q) || i.getName().toUpperCase().contains(q))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(dbResults.stream().limit(25).toList());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Instrument>> getPopularInstruments() {
        return ResponseEntity.ok(POPULAR_INDIAN_STOCKS);
    }
}
