package com.deepbank.common.exception;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String accountNumber) {
        super("Insufficient balance in account: " + accountNumber, "INSUFFICIENT_FUNDS");
    }
}
