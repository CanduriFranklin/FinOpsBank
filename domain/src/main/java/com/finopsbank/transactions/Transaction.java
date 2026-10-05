package com.finopsbank.transactions;

import java.time.LocalDateTime;

/**
 * Abstract base class for all transaction types.
 */
public abstract class Transaction {
    protected String transactionId;
    protected LocalDateTime date;
    protected double amount;
    protected String description;

    public Transaction(String transactionId, LocalDateTime date, double amount, String description) {
        this.transactionId = transactionId;
        this.date = date;
        this.amount = amount;
        this.description = description;
    }

    public LocalDateTime getDate() { return date; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
