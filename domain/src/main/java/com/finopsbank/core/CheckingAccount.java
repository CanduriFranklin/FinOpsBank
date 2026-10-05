package com.finopsbank.core;

/**
 * Checking account implementation.
 */
public class CheckingAccount extends Account {
    public CheckingAccount(String accountNumber, String customerId, double balance) {
        super(accountNumber, customerId, balance);
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        this.balance += amount;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive");
        if (amount > this.balance) throw new IllegalArgumentException("Insufficient funds");
        this.balance -= amount;
    }
}
