package com.stock.analysis.engine;

import com.stock.analysis.dto.OptionChainAnalysisDto;
import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.exception.MarketDataUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionChainAnalysisServiceTest {

    private OptionChainAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new OptionChainAnalysisService();
    }

    @Test
    void shouldThrowExceptionWhenSpotPriceInvalid() {
        List<StrikeDataDto> strikes = List.of(StrikeDataDto.builder().strikePrice(24000.0).callOi(100L).putOi(100L).build());
        assertThrows(MarketDataUnavailableException.class, () ->
                service.analyzeOptionChain("NIFTY", 0.0, strikes));
    }

    @Test
    void shouldThrowExceptionWhenCallOiZero() {
        List<StrikeDataDto> strikes = List.of(StrikeDataDto.builder().strikePrice(24000.0).callOi(0L).putOi(100L).build());
        assertThrows(MarketDataUnavailableException.class, () ->
                service.analyzeOptionChain("NIFTY", 24000.0, strikes));
    }

    @Test
    void shouldCalculatePcrAndMaxPainCorrectly() {
        List<StrikeDataDto> strikes = new ArrayList<>();
        strikes.add(StrikeDataDto.builder().strikePrice(24000.0).callOi(100000L).putOi(50000L).build());
        strikes.add(StrikeDataDto.builder().strikePrice(24100.0).callOi(80000L).putOi(120000L).build());

        OptionChainAnalysisDto dto = service.analyzeOptionChain("NIFTY", 24080.0, strikes);

        assertNotNull(dto);
        assertEquals(24100.0, dto.getAtmStrike());
        assertEquals(0.94, dto.getPutCallRatio());
        assertNotNull(dto.getMaxPainStrike());
    }
}
