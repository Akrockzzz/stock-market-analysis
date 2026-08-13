package com.stock.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private String symbol;
    private String name;
    private String exchange;
    private String instrumentKey;
    private String instrumentType;
}
