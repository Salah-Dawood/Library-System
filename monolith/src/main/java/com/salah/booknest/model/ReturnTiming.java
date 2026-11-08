package com.salah.booknest.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum ReturnTiming {
    EARLY,
    ON_TIME,
    LATE;

    public static ReturnTiming of(long daysLate) {
        if (daysLate < 0) {
            return EARLY;
        }
        return daysLate == 0 ? ON_TIME : LATE;
    }

    public static long daysLate(LocalDate due, LocalDate actual) {
        return due == null || actual == null ? 0 : ChronoUnit.DAYS.between(due, actual);
    }
}
