package com.stock.analysis.util;

import com.stock.analysis.enums.Exchange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public class ExpiryUtil {

    private static final Set<String> INDEX_SYMBOLS = Set.of(
            "NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "NIFTYNXT50", "SENSEX", "BANKEX"
    );

    public static boolean isIndexSymbol(String symbol) {
        if (symbol == null) return false;
        String clean = symbol.toUpperCase().replace("NSE_INDEX|", "").replace("BSE_INDEX|", "").trim();
        return INDEX_SYMBOLS.contains(clean);
    }

    public static LocalDate getNextValidExpiryDate(String symbol, Exchange exchange, ZonedDateTime zdt) {
        LocalDate date = zdt.toLocalDate();
        LocalTime time = zdt.toLocalTime();

        if (isIndexSymbol(symbol)) {
            // Index Options: Weekly Tuesdays for NSE, Weekly Thursdays for BSE
            DayOfWeek targetDay = (exchange == Exchange.BSE_EQ || exchange == Exchange.BSE_FO)
                    ? DayOfWeek.THURSDAY
                    : DayOfWeek.TUESDAY;

            if (date.getDayOfWeek() == targetDay && time.isBefore(LocalTime.of(15, 30))) {
                return date;
            }

            do {
                date = date.plusDays(1);
            } while (date.getDayOfWeek() != targetDay);

            return date;
        } else {
            // Single Stock Options: Monthly Last Thursday of the month
            LocalDate lastThursdayCurrentMonth = getLastThursdayOfMonth(date.getYear(), date.getMonthValue());

            if (date.isBefore(lastThursdayCurrentMonth)) {
                return lastThursdayCurrentMonth;
            } else if (date.isEqual(lastThursdayCurrentMonth) && time.isBefore(LocalTime.of(15, 30))) {
                return lastThursdayCurrentMonth;
            } else {
                // Past current month's last Thursday: resolve next month's last Thursday
                LocalDate nextMonth = date.plusMonths(1);
                return getLastThursdayOfMonth(nextMonth.getYear(), nextMonth.getMonthValue());
            }
        }
    }

    public static LocalDate getLastThursdayOfMonth(int year, int month) {
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        while (lastDayOfMonth.getDayOfWeek() != DayOfWeek.THURSDAY) {
            lastDayOfMonth = lastDayOfMonth.minusDays(1);
        }
        return lastDayOfMonth;
    }

    public static LocalDate getNextValidExpiryDate(String symbol, Exchange exchange) {
        return getNextValidExpiryDate(symbol, exchange, ZonedDateTime.now(MarketHoursUtil.IST_ZONE));
    }

    public static LocalDate getNextValidExpiryDate(String symbol) {
        return getNextValidExpiryDate(symbol, Exchange.NSE_EQ);
    }

    public static LocalDate getNextValidExpiryDate() {
        return getNextValidExpiryDate("NIFTY", Exchange.NSE_EQ);
    }
}
