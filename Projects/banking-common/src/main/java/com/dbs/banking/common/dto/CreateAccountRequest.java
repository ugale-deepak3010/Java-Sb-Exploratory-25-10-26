package com.dbs.banking.common.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        @NotBlank(message = "Currency is required (e.g. SGD, USD)")
        String currency,

        @NotNull(message = "Initial balance cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be >= 0")
        BigDecimal initialBalance
) {}
