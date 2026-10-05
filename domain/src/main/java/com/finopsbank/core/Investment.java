package com.finopsbank.core;

import java.time.LocalDate;

/**
 * Represents an investment associated with an account.
 */
public class Investment {
    private String investmentId;
    private String accountNumber;
    private String type;
    private double initialAmount;
    private LocalDate creationDate;

    public Investment(String investmentId, String accountNumber, String type, double initialAmount, LocalDate creationDate) {
        this.investmentId = investmentId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.initialAmount = initialAmount;
        this.creationDate = creationDate;
    }

    public String getInvestmentId() { return investmentId; }
    public String getAccountNumber() { return accountNumber; }
    public String getType() { return type; }
    public double getInitialAmount() { return initialAmount; }
    public LocalDate getCreationDate() { return creationDate; }
}
