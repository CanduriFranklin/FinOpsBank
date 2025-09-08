package com.finopsbank.business;

import com.finopsbank.core.Account;
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

    // TODO: Implement deposit, withdraw, transfer methods
}
