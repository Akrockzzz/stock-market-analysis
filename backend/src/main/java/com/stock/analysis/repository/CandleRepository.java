package com.stock.analysis.repository;

import com.stock.analysis.model.Candle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {
    List<Candle> findBySymbolAndIntervalNameOrderByTimestampAsc(String symbol, String intervalName);

    @Query("SELECT c FROM Candle c WHERE c.symbol = :symbol AND c.intervalName = :intervalName ORDER BY c.timestamp DESC")
    List<Candle> findRecentCandles(String symbol, String intervalName, Pageable pageable);

    List<Candle> findBySymbolAndIntervalNameAndTimestampBetweenOrderByTimestampAsc(
            String symbol, String intervalName, Instant start, Instant end);

    @Transactional
    @Modifying
    @Query("DELETE FROM Candle c WHERE c.symbol = :symbol AND c.intervalName = :intervalName AND c.timestamp BETWEEN :start AND :end")
    void deleteBySymbolAndIntervalNameAndTimestampBetween(String symbol, String intervalName, Instant start, Instant end);
}
