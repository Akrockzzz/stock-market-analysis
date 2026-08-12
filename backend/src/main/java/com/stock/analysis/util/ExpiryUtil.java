package com.stock.analysis.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class ExpiryUtil {

    public static LocalDate getNextValidThursdayExpiry(ZonedDateTime zdt) {
        LocalDate date = zdt.toLocalDate();
        LocalTime time = zdt.toLocalTime();

        if (date.getDayOfWeek() == DayOfWeek.THURSDAY && time.isBefore(LocalTime.of(15, 30))) {
            return date;
        }

        // Advance to next Thursday
        do {
            date = date.plusDays(1);
        } while (date.getDayOfWeek() != DayOfWeek.THURSDAY);

        return date;
    }

    public static LocalDate getNextValidThursdayExpiry() {
        return getNextValidThursdayExpiry(ZonedDateTime.now(MarketHoursUtil.IST_ZONE));
    }
}
