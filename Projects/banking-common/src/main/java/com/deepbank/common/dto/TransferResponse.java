package com.deepbank.common.dto;

import com.deepbank.common.enums.TransferStatus;
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
