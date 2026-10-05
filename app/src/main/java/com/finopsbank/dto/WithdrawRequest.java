package com.finopsbank.dto;

import java.math.BigDecimal;

public record WithdrawRequest(
    String accountNumber,
    BigDecimal amount
) {}