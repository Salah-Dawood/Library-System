package com.salah.booknest.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A librarian's verdict on a returned copy.
 *
 * @param damaged    true if the copy came back damaged
 * @param notes      what the librarian found (optional, shown to the member)
 * @param fineAmount optional fine to charge the member; omit or 0 for no fine
 */
public record ReviewReturnRequest(
        boolean damaged,
        @Size(max = 500) String notes,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal fineAmount) {
}
