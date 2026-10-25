package com.dbs.banking.common.dto;

import com.dbs.banking.common.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transferId,
        String idempotencyKey,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String message,
        Instant createdAt
) {}
