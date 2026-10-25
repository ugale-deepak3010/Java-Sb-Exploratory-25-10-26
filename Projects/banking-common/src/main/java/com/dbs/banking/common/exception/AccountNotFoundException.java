package com.dbs.banking.common.exception;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found for account number: " + accountNumber, "ACCOUNT_NOT_FOUND");
    }
}
