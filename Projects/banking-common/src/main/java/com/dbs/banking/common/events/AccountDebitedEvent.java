package com.dbs.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountDebitedEvent(
        String eventId,
        String transferId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        BigDecimal remainingBalance,
        Instant timestamp
) {}
