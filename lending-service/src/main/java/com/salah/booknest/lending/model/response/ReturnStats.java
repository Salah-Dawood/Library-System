package com.salah.booknest.lending.model.response;

public record ReturnStats(
        int returned,
        int early,
        int onTime,
        int late,
        int onTimeRate,
        boolean reliable) {
}
