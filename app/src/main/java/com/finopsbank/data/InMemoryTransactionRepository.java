package com.finopsbank.data;

import com.finopsbank.transactions.Transaction;
import java.util.*;

/**
 * In-memory implementation of TransactionRepository.
 */
@SuppressWarnings("unused")
public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<String, List<Transaction>> transactions = new HashMap<>();

    @Override
    public void save(String accountNumber, Transaction transaction) {
        transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>()).add(transaction);
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        return transactions.getOrDefault(accountNumber, Collections.emptyList());
    }
}
