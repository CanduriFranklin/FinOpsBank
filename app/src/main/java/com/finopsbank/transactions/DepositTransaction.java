package com.finopsbank.transactions;

import java.time.LocalDateTime;

/**
 * Represents a deposit transaction.
 */
public class DepositTransaction extends Transaction {
    public DepositTransaction(String transactionId, LocalDateTime date, double amount, String description) {
        super(transactionId, date, amount, description);
    }
}
