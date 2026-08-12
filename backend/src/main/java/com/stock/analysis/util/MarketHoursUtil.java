package com.stock.analysis.util;

import com.stock.analysis.enums.ConnectionState;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

@Component
public class MarketHoursUtil {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    public static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    // Official NSE Trading Holidays (Month, Day)
    private static final Set<String> NSE_HOLIDAYS_2026 = Set.of(
            "2026-01-26", // Republic Day
            "2026-02-17", // Mahashivratri
            "2026-03-03", // Holi
            "2026-04-03", // Good Friday
            "2026-04-14", // Dr. Baba Saheb Ambedkar Jayanti
            "2026-05-01", // Maharashtra Day
            "2026-05-28", // Bakri Id
            "2026-06-26", // Muharram
            "2026-08-15", // Independence Day
            "2026-10-02", // Mahatma Gandhi Jayanti
            "2026-10-20", // Dussehra
            "2026-11-09", // Diwali Balipratipada
            "2026-11-24", // Gurunanak Jayanti
            "2026-12-25"  // Christmas
    );

    public boolean isMarketOpenNow() {
        ZonedDateTime nowIst = ZonedDateTime.now(IST_ZONE);
        DayOfWeek day = nowIst.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalDate today = nowIst.toLocalDate();
        if (NSE_HOLIDAYS_2026.contains(today.toString())) {
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
