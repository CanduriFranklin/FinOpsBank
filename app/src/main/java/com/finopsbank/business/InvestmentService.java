package com.finopsbank.business;

import com.finopsbank.data.AccountRepository;

/**
 * Service for investment operations.
 */
public class InvestmentService {
    private final AccountRepository accountRepository;

    public InvestmentService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Create an investment for an account, withdrawing funds and returning the Investment object.
     */
    public com.finopsbank.core.Investment createInvestment(String accountNumber, String type, double amount) {
        var accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new com.finopsbank.exceptions.AccountNotFoundException("Account not found: " + accountNumber);
        }
        var account = accountOpt.get();
        if (amount > account.getBalance()) {
            throw new com.finopsbank.exceptions.InsufficientFundsException("Insufficient funds for investment");
        }
        account.withdraw(amount);
        accountRepository.save(account);
        var investment = new com.finopsbank.core.Investment(
            java.util.UUID.randomUUID().toString(),
            accountNumber,
            type,
            amount,
            java.time.LocalDate.now()
        );
        // In a real app, save investment to a repository
        return investment;
    }
}
