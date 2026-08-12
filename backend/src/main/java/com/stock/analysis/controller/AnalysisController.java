package com.stock.analysis.controller;

import com.stock.analysis.dto.OptionChainAnalysisDto;
import com.stock.analysis.dto.StrikeDataDto;
import com.stock.analysis.dto.TechnicalAnalysisDto;
import com.stock.analysis.engine.OptionChainAnalysisService;
import com.stock.analysis.engine.TechnicalAnalysisService;
import com.stock.analysis.exception.MarketDataUnavailableException;
import com.stock.analysis.model.Candle;
import com.stock.analysis.model.Tick;
import com.stock.analysis.repository.CandleRepository;
import com.stock.analysis.service.SpotPriceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalysisController {

    private final TechnicalAnalysisService technicalAnalysisService;
    private final OptionChainAnalysisService optionChainAnalysisService;
    private final CandleRepository candleRepository;
    private final SpotPriceCacheService spotPriceCacheService;

    @GetMapping("/technicals/{symbol}")
    public ResponseEntity<TechnicalAnalysisDto> getTechnicals(@PathVariable String symbol) {
        List<Candle> candles = candleRepository.findRecentCandles(symbol, "1d", PageRequest.of(0, 200));
        
        // If DB has no candles yet for new symbol, return simulated/mock structure for quick initial verification
        if (candles.isEmpty()) {
            candles = generateSampleCandles(symbol);
        }
        
        TechnicalAnalysisDto dto = technicalAnalysisService.calculateTechnicals(symbol, candles);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/option-chain/{symbol}")
    public ResponseEntity<OptionChainAnalysisDto> getOptionChain(
            @PathVariable String symbol,
            @RequestParam(required = false) Double spotPrice) {
        
        Double currentSpot = spotPrice;
        if (currentSpot == null) {
            currentSpot = spotPriceCacheService.getLatestTick(symbol)
                    .map(Tick::getLtp)
                    .orElse(24500.0); // Default NIFTY spot reference if cache empty
        }

        List<StrikeDataDto> strikes = generateSampleOptionChainStrikes(currentSpot);
        OptionChainAnalysisDto result = optionChainAnalysisService.analyzeOptionChain(symbol, currentSpot, strikes);
        return ResponseEntity.ok(result);
    }

    private List<Candle> generateSampleCandles(String symbol) {
        List<Candle> sample = new ArrayList<>();
        double price = 24000.0;
        java.time.Instant now = java.time.Instant.now();
        for (int i = 200; i >= 0; i--) {
            price += (Math.random() - 0.48) * 150;
            sample.add(Candle.builder()
                    .symbol(symbol)
                    .intervalName("1d")
                    .timestamp(now.minusSeconds((long) i * 86400))
                    .open(price - 20)
                    .high(price + 50)
                    .low(price - 40)
                    .close(price)
                    .volume(100000L + (long)(Math.random() * 50000))
                    .build());
        }
        return sample;
    }

    private List<StrikeDataDto> generateSampleOptionChainStrikes(double spotPrice) {
        List<StrikeDataDto> list = new ArrayList<>();
        double baseStrike = Math.floor(spotPrice / 100.0) * 100.0;
        for (int i = -10; i <= 10; i++) {
            double strike = baseStrike + (i * 100);
            long callOi = (long) (50000 + Math.max(0, 100000 - Math.abs(strike - spotPrice) * 100));
            long putOi = (long) (50000 + Math.max(0, 100000 - Math.abs(spotPrice - strike) * 100));
            list.add(StrikeDataDto.builder()
                    .strikePrice(strike)
                    .callOi(callOi)
                    .putOi(putOi)
                    .callLtp(Math.max(5.0, 300.0 - (strike - spotPrice)))
                    .putLtp(Math.max(5.0, 300.0 + (strike - spotPrice)))
                    .callIv(14.5 + Math.random() * 2)
                    .putIv(15.0 + Math.random() * 2)
                    .build());
        }
        return list;
    }
}
