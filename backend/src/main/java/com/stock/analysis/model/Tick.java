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
@Table(name = "ticks", indexes = {
    @Index(name = "idx_tick_key_time", columnList = "instrumentKey, timestamp")
})
public class Tick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instrumentKey;
    private String symbol;
    private Double ltp;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
    private Long openInterest;
    private Double bid;
    private Double ask;
    private Instant timestamp;
}
