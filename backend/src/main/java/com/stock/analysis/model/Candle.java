package com.stock.analysis.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "candles", indexes = {
    @Index(name = "idx_candle_sym_interval", columnList = "symbol, intervalName, timestamp")
})
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instrumentKey;
    private String symbol;
    private String intervalName; // 1m, 5m, 15m, 1d, 1w

    private Instant timestamp;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
    private Long openInterest;
}
