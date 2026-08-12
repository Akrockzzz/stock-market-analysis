package com.stock.analysis.repository;

import com.stock.analysis.model.OptionChainSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface OptionChainSnapshotRepository extends JpaRepository<OptionChainSnapshot, Long> {
    Optional<OptionChainSnapshot> findFirstByUnderlyingSymbolAndExpiryDateOrderByTimestampDesc(
            String underlyingSymbol, LocalDate expiryDate);
}
