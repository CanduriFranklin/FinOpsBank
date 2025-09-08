package com.finopsbank.core;

/**
 * Savings account implementation.
 */
public class SavingsAccount extends Account {
    public SavingsAccount(String accountNumber, String customerId, double balance) {
        super(accountNumber, customerId, balance);
    }

    @Override
    public void deposit(double amount) {
        // TODO: Implement deposit logic
    }

    @Override
    public void withdraw(double amount) {
        // TODO: Implement withdraw logic
    }

    public void applyInterest() {
        // TODO: Implement interest logic
    }
}
