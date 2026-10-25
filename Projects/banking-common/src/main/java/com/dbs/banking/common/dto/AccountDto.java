package com.dbs.banking.common.dto;

import com.dbs.banking.common.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountDto(
        Long id,
        String accountNumber,
        String customerId,
        String accountHolderName,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
