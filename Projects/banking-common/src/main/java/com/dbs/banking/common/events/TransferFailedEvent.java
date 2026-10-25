package com.dbs.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferFailedEvent(
        String eventId,
        String transferId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String reason,
        boolean compensationRequired,
        Instant failedAt
) {}
