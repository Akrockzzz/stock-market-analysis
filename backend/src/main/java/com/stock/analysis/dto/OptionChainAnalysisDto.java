package com.stock.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionChainAnalysisDto {
    private String underlyingSymbol;
    private Double spotPrice;
    private Double atmStrike;
    private Long totalCallOi;
    private Long totalPutOi;
    private Double putCallRatio;
    private Double maxPainStrike;
    private List<StrikeDataDto> strikes;
}
