package com.finopsbank.performance;

import com.finopsbank.core.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AccountConcurrencyStressTest {

    @Test
    @DisplayName("Stress Test Concurrencia Java 25: 500 Virtual Threads compitiendo sobre la misma cuenta")
    void testConcurrentWithdrawalsWithVirtualThreads() throws InterruptedException {
        int numberOfThreads = 500;
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal withdrawalAmount = new BigDecimal("10.00");

        Account sharedAccount = new Account("ACC-STRESS-001", initialBalance);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulWithdrawals = new AtomicInteger(0);

        // Usando Virtual Threads nativos de Java 25
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        synchronized (sharedAccount) {
                            if (sharedAccount.getBalance().compareTo(withdrawalAmount) >= 0) {
                                sharedAccount.withdraw(withdrawalAmount);
                                successfulWithdrawals.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();

        // Verificacion: Solo debieron tener exito exactamente 100 de los 500 intentos (100 * 10 = 1000)
        assertThat(successfulWithdrawals.get()).isEqualTo(100);
        assertThat(sharedAccount.getBalance()).isEqualByComparingTo("0.00");
    }
}