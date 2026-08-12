package com.stock.analysis.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "option_chain_snapshots")
public class OptionChainSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String underlyingSymbol;
    private LocalDate expiryDate;
    private Double spotPrice;
    private Double atmStrike;

    private Double putCallRatio;
    private Double maxPainStrike;

    @Column(columnDefinition = "TEXT")
    private String jsonStrikeData; // Array of strike details (Call OI, Put OI, Call IV, Put IV)

    private Instant timestamp;
}
