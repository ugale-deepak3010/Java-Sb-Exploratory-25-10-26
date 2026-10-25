package com.dbs.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferRequestedEvent(
        String eventId,
        String transferId,
        String idempotencyKey,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
