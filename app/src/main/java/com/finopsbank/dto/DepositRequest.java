package com.finopsbank.dto;

import java.math.BigDecimal;

public record DepositRequest(
    String accountNumber,
    BigDecimal amount
) {}