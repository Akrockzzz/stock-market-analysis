package com.stock.analysis.repository;

import com.stock.analysis.model.Candle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {
    List<Candle> findBySymbolAndIntervalNameOrderByTimestampAsc(String symbol, String intervalName);

    @Query("SELECT c FROM Candle c WHERE c.symbol = :symbol AND c.intervalName = :intervalName ORDER BY c.timestamp DESC")
    List<Candle> findRecentCandles(String symbol, String intervalName, Pageable pageable);

    List<Candle> findBySymbolAndIntervalNameAndTimestampBetweenOrderByTimestampAsc(
            String symbol, String intervalName, Instant start, Instant end);
}
