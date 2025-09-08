package com.finopsbank.transactions;

import java.time.LocalDateTime;

/**
 * Represents a transfer transaction (e.g., PIX).
 */
public class TransferTransaction extends Transaction {
    private String fromAccount;
    private String toAccount;

    public TransferTransaction(String transactionId, LocalDateTime date, double amount, String description, String fromAccount, String toAccount) {
        super(transactionId, date, amount, description);
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
    }

    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
}
