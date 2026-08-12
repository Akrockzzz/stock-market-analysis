package com.stock.analysis.repository;

import com.stock.analysis.model.StockScorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockScorecardRepository extends JpaRepository<StockScorecard, String> {
    Optional<StockScorecard> findBySymbol(String symbol);
}
