package com.stock.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrikeDataDto {
    private Double strikePrice;
    private Long callOi;
    private Long putOi;
    private Long callChangeOi;
    private Long putChangeOi;
    private Double callLtp;
    private Double putLtp;
    private Double callIv;
    private Double putIv;
}
