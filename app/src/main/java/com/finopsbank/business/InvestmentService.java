package com.finopsbank.business;

import com.finopsbank.core.Investment;
import com.finopsbank.data.AccountRepository;

/**
 * Service for investment operations.
 */
public class InvestmentService {
    private final AccountRepository accountRepository;

    public InvestmentService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // TODO: Implement createInvestment method
}
