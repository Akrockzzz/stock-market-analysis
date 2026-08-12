package com.stock.analysis.repository;

import com.stock.analysis.model.Fundamentals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundamentalsRepository extends JpaRepository<Fundamentals, String> {
    Optional<Fundamentals> findBySymbol(String symbol);
}
