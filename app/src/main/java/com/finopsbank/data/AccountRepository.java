package com.finopsbank.data;

import com.finopsbank.core.Account;
import java.util.Optional;

/**
 * Interface for account repository operations.
 */
public interface AccountRepository {
    void save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
    void delete(String accountNumber);
}
