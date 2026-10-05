package com.finopsbank.dto;

import java.math.BigDecimal;

public record TransferRequest(
    String sourceAccountNumber,
    String targetAccountNumber,
    BigDecimal amount
) {}