package com.stock.analysis.engine;

import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Candle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TechnicalAnalysisServiceTest {

    private TechnicalAnalysisService technicalAnalysisService;

    @BeforeEach
    void setUp() {
        technicalAnalysisService = new TechnicalAnalysisService();
    }

    @Test
    void shouldThrowExceptionWhenInsufficientCandles() {
        List<Candle> insufficient = new ArrayList<>();
        insufficient.add(Candle.builder().close(100.0).build());

        assertThrows(MarketDataUnavailableException.class, () ->
                technicalAnalysisService.calculateTechnicals("RELIANCE", insufficient));
    }

    @Test
    void shouldCalculateRsiAndEmaSuccessfully() {
        List<Candle> candles = new ArrayList<>();
        double price = 2000.0;
        Instant now = Instant.now();

        for (int i = 50; i >= 0; i--) {
            price += (i % 2 == 0) ? 10.0 : -5.0;
            candles.add(Candle.builder()
                    .symbol("RELIANCE")
                    .intervalName("1d")
                    .timestamp(now.minusSeconds(i * 86400))
                    .open(price - 5)
                    .high(price + 15)
                    .low(price - 10)
                    .close(price)
                    .volume(50000L)
                    .build());
        }

        TechnicalAnalysisDto dto = technicalAnalysisService.calculateTechnicals("RELIANCE", candles);

        assertNotNull(dto);
        assertEquals("RELIANCE", dto.getSymbol());
        assertNotNull(dto.getRsi14());
        assertTrue(dto.getRsi14() >= 0 && dto.getRsi14() <= 100);
        assertNotNull(dto.getEma20());
        assertNotNull(dto.getPivotPoint());
    }
}
