package com.salah.booknest.model.response;

public record ReturnStats(
        int returned,
        int early,
        int onTime,
        int late,
        int onTimeRate,
        boolean reliable) {
}
