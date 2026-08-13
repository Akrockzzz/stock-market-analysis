package com.stock.analysis.controller;

import com.stock.analysis.dto.SearchResultDto;
import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import com.stock.analysis.model.Instrument;
import com.stock.analysis.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/instruments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InstrumentSearchController {

    private final InstrumentRepository instrumentRepository;

    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchInstruments(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "true") boolean equitiesOnly) {
        
        String q = query.trim().toUpperCase();
        if (q.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Instrument> results = instrumentRepository.findBySymbolContainingIgnoreCase(q);

        // Filter for equities by default to avoid flooding search with F&O option contracts
        if (equitiesOnly) {
            results = results.stream()
                    .filter(i -> i.getExchange() == Exchange.NSE_EQ || 
                                 i.getInstrumentType() == InstrumentType.EQUITY)
                    .collect(Collectors.toList());
        }

        // Smart ranking:
        // 1. Exact symbol match first
        // 2. Symbol starts with query
        // 3. Shortest symbol length
        // 4. Alphabetical by symbol
        Comparator<Instrument> ranker = (a, b) -> {
            String symA = a.getSymbol() != null ? a.getSymbol().toUpperCase() : "";
            String symB = b.getSymbol() != null ? b.getSymbol().toUpperCase() : "";

            boolean exactA = symA.equals(q);
            boolean exactB = symB.equals(q);
            if (exactA && !exactB) return -1;
            if (!exactA && exactB) return 1;

            boolean startA = symA.startsWith(q);
            boolean startB = symB.startsWith(q);
            if (startA && !startB) return -1;
            if (!startA && startB) return 1;

            int lenComp = Integer.compare(symA.length(), symB.length());
            if (lenComp != 0) return lenComp;

            return symA.compareTo(symB);
        };

        List<SearchResultDto> dtos = results.stream()
                .sorted(ranker)
                .limit(25)
                .map(this::toSearchResultDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getInstrumentCount() {
        long count = instrumentRepository.count();
        return ResponseEntity.ok(Map.of(
                "count", count,
                "status", count > 0 ? "POPULATED" : "EMPTY"
        ));
    }

    private SearchResultDto toSearchResultDto(Instrument instrument) {
        return SearchResultDto.builder()
                .symbol(instrument.getSymbol())
                .name(instrument.getName() != null ? instrument.getName() : instrument.getSymbol())
                .exchange(instrument.getExchange() != null ? instrument.getExchange().name() : "NSE_EQ")
                .instrumentKey(instrument.getInstrumentKey())
                .instrumentType(instrument.getInstrumentType() != null ? instrument.getInstrumentType().name() : "EQUITY")
                .build();
    }
}
