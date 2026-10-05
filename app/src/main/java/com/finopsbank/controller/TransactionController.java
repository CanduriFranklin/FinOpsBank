package com.finopsbank.controller;

import com.finopsbank.dto.DepositRequest;
import com.finopsbank.dto.TransferRequest;
import com.finopsbank.dto.WithdrawRequest;
import com.finopsbank.persistence.entity.AccountEntity;
import com.finopsbank.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final AccountService accountService;

    public TransactionController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<AccountEntity> deposit(@RequestBody DepositRequest request) {
        return ResponseEntity.ok(accountService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<AccountEntity> withdraw(@RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(accountService.withdraw(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(request));
    }
}