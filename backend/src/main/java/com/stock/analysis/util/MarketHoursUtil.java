package com.stock.analysis.util;

import com.stock.analysis.enums.ConnectionState;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Component
public class MarketHoursUtil {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    public static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    public boolean isMarketOpenNow() {
        ZonedDateTime nowIst = ZonedDateTime.now(IST_ZONE);
        DayOfWeek day = nowIst.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = nowIst.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }

    public ConnectionState determineSystemConnectionState(boolean hasValidToken, boolean isWsConnected) {
        if (!hasValidToken) {
            return ConnectionState.NOT_CONNECTED;
        }
        if (isWsConnected && isMarketOpenNow()) {
            return ConnectionState.LIVE;
        }
        return ConnectionState.HISTORICAL_ONLY;
    }
}
