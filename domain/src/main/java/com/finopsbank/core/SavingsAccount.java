package com.finopsbank.core;

/**
 * Savings account implementation.
 */
public class SavingsAccount extends Account {
    private double interestRate;

    public SavingsAccount(String accountNumber, String customerId, double balance, double interestRate) {
        super(accountNumber, customerId, balance);
        this.interestRate = interestRate;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
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

    public void applyInterest() {
        this.balance += this.balance * interestRate;
    }
}
