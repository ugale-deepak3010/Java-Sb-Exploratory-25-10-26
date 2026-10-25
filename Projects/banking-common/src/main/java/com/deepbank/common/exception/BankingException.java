package com.deepbank.common.exception;

public class BankingException extends RuntimeException {
	
    private static final long serialVersionUID = 1L;
    
	private final String errorCode;

    public BankingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankingException(String message) {
        super(message);
        this.errorCode = "BANKING_GENERAL_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
