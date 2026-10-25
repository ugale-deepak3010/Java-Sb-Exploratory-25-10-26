package com.deepbank.account.service;

import com.deepbank.account.entity.TransactionLedger;
import com.deepbank.common.dto.AccountDto;
import com.deepbank.common.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto createAccount(CreateAccountRequest request);
    AccountDto getAccount(String accountNumber);
    AccountDto debit(String transferId, String accountNumber, BigDecimal amount, String remarks);
    AccountDto credit(String transferId, String accountNumber, BigDecimal amount, String remarks);
    List<TransactionLedger> getLedger(String accountNumber);
}
