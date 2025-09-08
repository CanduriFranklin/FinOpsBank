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
        // TODO: Implement deposit logic
    }

    @Override
    public void withdraw(double amount) {
        // TODO: Implement withdraw logic
    }
}
