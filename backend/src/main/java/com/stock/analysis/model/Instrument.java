package com.stock.analysis.model;

import com.stock.analysis.enums.Exchange;
import com.stock.analysis.enums.InstrumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "instruments", indexes = {
    @Index(name = "idx_symbol", columnList = "symbol"),
    @Index(name = "idx_underlying", columnList = "underlyingSymbol")
})
public class Instrument {

    @Id
    private String instrumentKey; // e.g. NSE_EQ|INE002A01018 or NSE_FO|54321

    @Enumerated(EnumType.STRING)
    private Exchange exchange;

    private String symbol;
    private String name;

    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType;

    private Integer lotSize;
    private Double strikePrice;
    private LocalDate expiry;
    private Double tickSize;
    private String underlyingSymbol;
}
