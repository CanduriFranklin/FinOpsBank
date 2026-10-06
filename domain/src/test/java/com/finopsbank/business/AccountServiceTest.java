package com.finopsbank.business;

import com.finopsbank.core.Account;
import com.finopsbank.data.AccountRepository;
import com.finopsbank.exceptions.AccountNotFoundException;
import com.finopsbank.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Debe lanzar InsufficientFundsException cuando el saldo es inferior al monto a debitar")
    void shouldThrowExceptionWhenBalanceIsInsufficient() {
        // Given
        String accountNumber = "ACC-100200300";
        Account account = new Account(accountNumber, new BigDecimal("50.00"));
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.withdraw(accountNumber, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe realizar el debito exitosamente cuando hay fondos suficientes")
    void shouldWithdrawSuccessfully() {
        // Given
        String accountNumber = "ACC-100200300";
        Account account = new Account(accountNumber, new BigDecimal("200.00"));
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        // When
        accountService.withdraw(accountNumber, new BigDecimal("50.00"));

        // Then
        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository, times(1)).save(account);
    }
}