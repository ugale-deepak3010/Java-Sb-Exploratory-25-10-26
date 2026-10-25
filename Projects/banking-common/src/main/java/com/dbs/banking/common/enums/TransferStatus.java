package com.dbs.banking.common.enums;

public enum TransferStatus {
    INITIATED,
    PENDING_DEBIT,
    DEBIT_FAILED,
    PENDING_CREDIT,
    CREDIT_FAILED,
    SETTLED,
    COMPENSATED,
    FAILED
}
