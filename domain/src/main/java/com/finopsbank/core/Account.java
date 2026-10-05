package com.finopsbank.core;

/**
 * Abstract base class for all account types.
 */
public abstract class Account {
    protected String accountNumber;
    protected double balance;
    protected String customerId;

    public Account(String accountNumber, String customerId, double balance) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = balance;
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public String getCustomerId() { return customerId; }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public abstract void deposit(double amount);
    public abstract void withdraw(double amount);
}
