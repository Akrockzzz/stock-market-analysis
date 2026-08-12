package com.stock.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockMarketAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockMarketAnalysisApplication.class, args);
    }
}
