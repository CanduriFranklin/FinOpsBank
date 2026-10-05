package com.finopsbank.service;

import com.finopsbank.dto.CreateAccountRequest;
import com.finopsbank.dto.DepositRequest;
import com.finopsbank.dto.TransferRequest;
import com.finopsbank.dto.WithdrawRequest;
import com.finopsbank.messaging.TransactionEventPublisher;
import com.finopsbank.persistence.entity.AccountEntity;
import com.finopsbank.persistence.repository.SpringDataAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final SpringDataAccountRepository accountRepository;
    private final TransactionEventPublisher eventPublisher;

    public AccountService(SpringDataAccountRepository accountRepository, TransactionEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountEntity createAccount(CreateAccountRequest request) {
        String type = (request.accountType() != null) ? request.accountType() : "SAVINGS";
        AccountEntity account = new AccountEntity(
                request.accountNumber(),
                request.ownerName(),
                request.initialBalance(),
                type
        );
        AccountEntity saved = accountRepository.save(account);
        eventPublisher.publishEvent(saved.getAccountNumber(), "ACCOUNT_CREATED: Initial balance " + saved.getBalance());
        return saved;
    }

    public List<AccountEntity> getAllAccounts() {
        return accountRepository.findAll();
    }

    public AccountEntity getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + accountNumber));
    }

    @Transactional
    public AccountEntity deposit(DepositRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a depositar debe ser mayor a cero");
        }
        AccountEntity account = getAccountByNumber(request.accountNumber());
        account.setBalance(account.getBalance().add(request.amount()));
        AccountEntity updated = accountRepository.save(account);

        eventPublisher.publishEvent(updated.getAccountNumber(), "DEPOSIT_COMPLETED: Amount " + request.amount() + " | New Balance " + updated.getBalance());
        return updated;
    }

    @Transactional
    public AccountEntity withdraw(WithdrawRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a cero");
        }
        AccountEntity account = getAccountByNumber(request.accountNumber());
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Saldo insuficiente en la cuenta: " + request.accountNumber());
        }
        account.setBalance(account.getBalance().subtract(request.amount()));
        AccountEntity updated = accountRepository.save(account);

        eventPublisher.publishEvent(updated.getAccountNumber(), "WITHDRAWAL_COMPLETED: Amount " + request.amount() + " | New Balance " + updated.getBalance());
        return updated;
    }

    @Transactional
    public String transfer(TransferRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a transferir debe ser mayor a cero");
        }

        withdraw(new WithdrawRequest(request.sourceAccountNumber(), request.amount()));
        deposit(new DepositRequest(request.targetAccountNumber(), request.amount()));

        eventPublisher.publishEvent(request.sourceAccountNumber(), "TRANSFER_SENT: To " + request.targetAccountNumber() + " | Amount " + request.amount());
        eventPublisher.publishEvent(request.targetAccountNumber(), "TRANSFER_RECEIVED: From " + request.sourceAccountNumber() + " | Amount " + request.amount());

        return "Transferencia realizada con éxito";
    }
}