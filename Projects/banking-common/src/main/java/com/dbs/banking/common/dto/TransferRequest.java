package com.dbs.banking.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotBlank(message = "Source account number is required")
        String sourceAccountNumber,

        @NotBlank(message = "Destination account number is required")
        String destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Transfer amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required (e.g. SGD)")
        String currency,

        String referenceNotes
) {}
