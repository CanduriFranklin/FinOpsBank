package com.finopsbank.business;

import com.finopsbank.data.AccountRepository;
import com.finopsbank.data.TransactionRepository;

/**
 * Service for account operations (deposit, withdraw, transfer).
 */
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Deposit funds into an account and record the transaction.
     */
    public void deposit(String accountNumber, double amount) {
        var accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new com.finopsbank.exceptions.AccountNotFoundException("Account not found: " + accountNumber);
        }
        var account = accountOpt.get();
        account.deposit(amount);
        accountRepository.save(account);
        var transaction = new com.finopsbank.transactions.DepositTransaction(
            java.util.UUID.randomUUID().toString(),
            java.time.LocalDateTime.now(),
            amount,
            "Deposit"
        );
        transactionRepository.save(accountNumber, transaction);
    }

    /**
     * Withdraw funds from an account and record the transaction.
     */
    public void withdraw(String accountNumber, double amount) {
        var accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new com.finopsbank.exceptions.AccountNotFoundException("Account not found: " + accountNumber);
        }
        var account = accountOpt.get();
        if (amount > account.getBalance()) {
            throw new com.finopsbank.exceptions.InsufficientFundsException("Insufficient funds");
        }
        account.withdraw(amount);
        accountRepository.save(account);
        var transaction = new com.finopsbank.transactions.WithdrawalTransaction(
            java.util.UUID.randomUUID().toString(),
            java.time.LocalDateTime.now(),
            amount,
            "Withdrawal"
        );
        transactionRepository.save(accountNumber, transaction);
    }

    /**
     * Transfer funds between accounts (PIX transfer) and record the transaction.
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new com.finopsbank.exceptions.InvalidOperationException("Cannot transfer to the same account");
        }
        var fromOpt = accountRepository.findByAccountNumber(fromAccountNumber);
        var toOpt = accountRepository.findByAccountNumber(toAccountNumber);
        if (fromOpt.isEmpty() || toOpt.isEmpty()) {
            throw new com.finopsbank.exceptions.AccountNotFoundException("One or both accounts not found");
        }
        var from = fromOpt.get();
        var to = toOpt.get();
        if (amount > from.getBalance()) {
            throw new com.finopsbank.exceptions.InsufficientFundsException("Insufficient funds");
        }
        from.withdraw(amount);
        to.deposit(amount);
        accountRepository.save(from);
        accountRepository.save(to);
        var transaction = new com.finopsbank.transactions.TransferTransaction(
            java.util.UUID.randomUUID().toString(),
            java.time.LocalDateTime.now(),
            amount,
            "PIX Transfer",
            fromAccountNumber,
            toAccountNumber
        );
        transactionRepository.save(fromAccountNumber, transaction);
        transactionRepository.save(toAccountNumber, transaction);
    }
}
