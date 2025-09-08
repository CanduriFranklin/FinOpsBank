package com.finopsbank.business;

import com.finopsbank.data.TransactionRepository;

/**
 * Service for transaction history queries.
 */
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // TODO: Implement getTransactionHistory method
}
