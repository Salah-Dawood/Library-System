package com.salah.booknest.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FineResponse {
    private Long id;
    private Long loanId;
    private String type;
    private String status;
    private BigDecimal amount;
    private String reason;
}
