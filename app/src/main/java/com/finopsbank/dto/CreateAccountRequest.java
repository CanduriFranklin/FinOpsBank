package com.finopsbank.dto;

import java.math.BigDecimal;

public record CreateAccountRequest(
    String accountNumber,
    String ownerName,
    BigDecimal initialBalance,
    String accountType
) {}