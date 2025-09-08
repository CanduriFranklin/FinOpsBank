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
        // TODO: Implement test
    }

    @Test
    void testWithdraw() {
        // TODO: Implement test
    }
}
