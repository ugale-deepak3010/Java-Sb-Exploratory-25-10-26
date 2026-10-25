package com.dbs.banking.common.exception;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String accountNumber) {
        super("Insufficient balance in account: " + accountNumber, "INSUFFICIENT_FUNDS");
    }
}
