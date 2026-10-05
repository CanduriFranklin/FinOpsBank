package com.finopsbank.transactions;

import java.time.LocalDateTime;

/**
 * Represents a transfer transaction (e.g., PIX).
 */
public class TransferTransaction extends Transaction {

    public TransferTransaction(String transactionId, LocalDateTime date, double amount, String description) {
        super(transactionId, date, amount, description);
    }

}
