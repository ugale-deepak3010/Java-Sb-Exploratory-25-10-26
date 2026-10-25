package com.dbs.banking.account.service;

import com.dbs.banking.account.entity.TransactionLedger;
import com.dbs.banking.common.dto.AccountDto;
import com.dbs.banking.common.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto createAccount(CreateAccountRequest request);
    AccountDto getAccount(String accountNumber);
    AccountDto debit(String transferId, String accountNumber, BigDecimal amount, String remarks);
    AccountDto credit(String transferId, String accountNumber, BigDecimal amount, String remarks);
    List<TransactionLedger> getLedger(String accountNumber);
}
