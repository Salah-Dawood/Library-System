package com.salah.booknest.demo;

public record RaceResult(
        boolean safe,
        int threads,
        int copiesAtStart,
        int membersWhoGotACopy,
        int availableCopiesAfter,
        String verdict) {
}
