package com.stock.analysis.util;

import com.stock.analysis.enums.Exchange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class ExpiryUtil {

    public static LocalDate getNextValidExpiryDate(Exchange exchange, ZonedDateTime zdt) {
        LocalDate date = zdt.toLocalDate();
        LocalTime time = zdt.toLocalTime();

        // SEBI Rule: NSE expiries on Tuesday, BSE expiries on Thursday
        DayOfWeek targetDay = (exchange == Exchange.BSE_EQ || exchange == Exchange.BSE_FO)
                ? DayOfWeek.THURSDAY
                : DayOfWeek.TUESDAY;

        if (date.getDayOfWeek() == targetDay && time.isBefore(LocalTime.of(15, 30))) {
            return date;
        }

        // Advance to next target expiry day
        do {
            date = date.plusDays(1);
        } while (date.getDayOfWeek() != targetDay);

        return date;
    }

    public static LocalDate getNextValidExpiryDate(Exchange exchange) {
        return getNextValidExpiryDate(exchange, ZonedDateTime.now(MarketHoursUtil.IST_ZONE));
    }

    public static LocalDate getNextValidExpiryDate() {
        return getNextValidExpiryDate(Exchange.NSE_EQ);
    }
}
