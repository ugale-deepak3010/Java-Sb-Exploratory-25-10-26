package com.deepbank.account.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deepbank.account.entity.TransactionLedger;
import com.deepbank.account.service.AccountService;
import com.deepbank.common.dto.AccountDto;
import com.deepbank.common.dto.ApiResponse;
import com.deepbank.common.dto.CreateAccountRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
    	
        AccountDto dto = accountService.createAccount(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", dto));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(@PathVariable("accountNumber") String accountNumber) {
    	
        AccountDto dto = accountService.getAccount(accountNumber);
        
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<ApiResponse<AccountDto>> debitAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("transferId") String transferId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(name = "remarks", required = false, defaultValue = "Manual debit") String remarks) {
        AccountDto dto = accountService.debit(transferId, accountNumber, amount, remarks);
        return ResponseEntity.ok(ApiResponse.ok("Debited successfully", dto));
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<ApiResponse<AccountDto>> creditAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("transferId") String transferId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(name = "remarks", required = false, defaultValue = "Manual credit") String remarks) {
        AccountDto dto = accountService.credit(transferId, accountNumber, amount, remarks);
        return ResponseEntity.ok(ApiResponse.ok("Credited successfully", dto));
    }

    @GetMapping("/{accountNumber}/ledger")
    public ResponseEntity<ApiResponse<List<TransactionLedger>>> getLedger(@PathVariable("accountNumber") String accountNumber) {
        List<TransactionLedger> ledger = accountService.getLedger(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok(ledger));
    }
}
