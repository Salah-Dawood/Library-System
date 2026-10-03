package com.salah.booknest.model;

import java.util.Set;

public enum LoanStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    RETURNED;

    private Set<LoanStatus> allowedNext() {
        return switch (this) {
            case REQUESTED -> Set.of(APPROVED, REJECTED, CANCELLED);
            case APPROVED -> Set.of(RETURNED, CANCELLED);
            case REJECTED, CANCELLED, RETURNED -> Set.of();
        };
    }

    public boolean canChangeTo(LoanStatus next) {
        return allowedNext().contains(next);
    }
}
