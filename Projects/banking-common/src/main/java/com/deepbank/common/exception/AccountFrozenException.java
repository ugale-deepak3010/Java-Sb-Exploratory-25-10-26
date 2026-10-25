package com.deepbank.common.exception;

public class AccountFrozenException extends BankingException {
    public AccountFrozenException(String accountNumber) {
        super("Account is frozen/suspended: " + accountNumber, "ACCOUNT_FROZEN");
    }
}
