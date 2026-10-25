package com.deepbank.common.exception;

public class DuplicateTransactionException extends BankingException {
    public DuplicateTransactionException(String idempotencyKey) {
        super("Duplicate transaction detected with idempotency key: " + idempotencyKey, "DUPLICATE_TRANSACTION");
    }
}
