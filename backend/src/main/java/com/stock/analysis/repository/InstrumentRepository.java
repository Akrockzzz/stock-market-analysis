package com.stock.analysis.repository;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import com.stock.analysis.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, String> {
    Optional<Instrument> findBySymbolAndExchange(String symbol, Exchange exchange);
    List<Instrument> findByUnderlyingSymbolAndInstrumentType(String underlyingSymbol, InstrumentType instrumentType);
    List<Instrument> findByUnderlyingSymbolAndExpiry(String underlyingSymbol, LocalDate expiry);
    List<Instrument> findBySymbolContainingIgnoreCase(String query);
}
