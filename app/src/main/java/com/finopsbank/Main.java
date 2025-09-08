package com.finopsbank;

import com.finopsbank.business.*;
import com.finopsbank.data.*;

/**
 * Main entry point for the FinOpsBank console application.
 */
public class Main {
    public static void main(String[] args) {
        // Instantiate repositories
        AccountRepository accountRepository = new InMemoryAccountRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();

        // Instantiate services
        AccountService accountService = new AccountService(accountRepository, transactionRepository);
        InvestmentService investmentService = new InvestmentService(accountRepository);
        TransactionService transactionService = new TransactionService(transactionRepository);

        // TODO: Implement console menu and user interaction
    }
}
