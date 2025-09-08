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

    /**
     * Retrieve the transaction history for a given account.
     */
    public java.util.List<com.finopsbank.transactions.Transaction> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }
}
