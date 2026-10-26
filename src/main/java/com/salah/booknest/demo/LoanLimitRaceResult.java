package com.salah.booknest.demo;

public record LoanLimitRaceResult(
        String username,
        boolean safe,
        int threads,
        int limit,
        int loansBefore,
        int borrowedDuringRace,
        int loansAfter,
        String verdict) {
}
