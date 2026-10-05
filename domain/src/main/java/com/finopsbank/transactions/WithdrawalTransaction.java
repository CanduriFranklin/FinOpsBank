package com.finopsbank.transactions;

import java.time.LocalDateTime;

/**
 * Represents a withdrawal transaction.
 */
public class WithdrawalTransaction extends Transaction {
    public WithdrawalTransaction(String transactionId, LocalDateTime date, double amount, String description) {
        super(transactionId, date, amount, description);
    }
}
