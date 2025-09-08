package com.finopsbank.test;

import com.finopsbank.business.AccountService;
import com.finopsbank.data.InMemoryAccountRepository;
import com.finopsbank.data.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountService.
 */
public class AccountServiceTest {
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(new InMemoryAccountRepository(), new InMemoryTransactionRepository());
    }

    @Test
    void testDeposit() {
        // Arrange
        String accountNumber = "123";
        com.finopsbank.core.CheckingAccount account = new com.finopsbank.core.CheckingAccount(accountNumber, "cust1", 100.0);
        accountService.deposit(accountNumber, 50.0); // Should not work yet, as account is not saved
        // Save account first
        accountService = new AccountService(new com.finopsbank.data.InMemoryAccountRepository(), new com.finopsbank.data.InMemoryTransactionRepository());
        com.finopsbank.data.AccountRepository repo = new com.finopsbank.data.InMemoryAccountRepository();
        repo.save(account);
        accountService = new AccountService(repo, new com.finopsbank.data.InMemoryTransactionRepository());

        // Act
        accountService.deposit(accountNumber, 50.0);

        // Assert
        com.finopsbank.core.Account updated = repo.findByAccountNumber(accountNumber).get();
        org.junit.jupiter.api.Assertions.assertEquals(150.0, updated.getBalance(), 0.001);
    }

    @Test
    void testWithdraw() {
        // Arrange
        String accountNumber = "456";
        com.finopsbank.core.CheckingAccount account = new com.finopsbank.core.CheckingAccount(accountNumber, "cust2", 200.0);
        com.finopsbank.data.AccountRepository repo = new com.finopsbank.data.InMemoryAccountRepository();
        repo.save(account);
        accountService = new AccountService(repo, new com.finopsbank.data.InMemoryTransactionRepository());

        // Act
        accountService.withdraw(accountNumber, 80.0);

        // Assert
        com.finopsbank.core.Account updated = repo.findByAccountNumber(accountNumber).get();
        org.junit.jupiter.api.Assertions.assertEquals(120.0, updated.getBalance(), 0.001);
    }
}
