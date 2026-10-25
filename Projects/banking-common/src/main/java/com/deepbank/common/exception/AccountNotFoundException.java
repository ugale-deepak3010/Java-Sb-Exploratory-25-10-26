package com.deepbank.common.exception;

public class AccountNotFoundException extends BankingException {
	
    private static final long serialVersionUID = 1L;

	public AccountNotFoundException(String accountNumber) {
        super("Account not found for account number: " + accountNumber, "ACCOUNT_NOT_FOUND");
    }
}
