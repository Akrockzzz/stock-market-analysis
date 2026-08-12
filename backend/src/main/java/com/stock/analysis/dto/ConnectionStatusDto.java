package com.stock.analysis.dto;

import com.stock.analysis.enums.ConnectionState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStatusDto {
    private String source;
    private ConnectionState state;
    private String message;
    private Instant lastHeartbeat;
}
