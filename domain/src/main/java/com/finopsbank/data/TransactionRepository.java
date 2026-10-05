package com.finopsbank.data;

import com.finopsbank.transactions.Transaction;
import java.util.List;

/**
 * Interface for transaction repository operations.
 */
public interface TransactionRepository {
    void save(String accountNumber, Transaction transaction);
    List<Transaction> findByAccountNumber(String accountNumber);
}
