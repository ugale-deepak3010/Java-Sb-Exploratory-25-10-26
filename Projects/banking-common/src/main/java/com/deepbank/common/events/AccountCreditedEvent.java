package com.deepbank.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountCreditedEvent(
        String eventId,
        String transferId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
