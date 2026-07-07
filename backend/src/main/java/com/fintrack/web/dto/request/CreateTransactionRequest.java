package com.fintrack.web.dto.request;

import com.fintrack.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(

    @NotNull(message = "Account is required")
    UUID accountId,

    // categoryId is optional — user can add it later
    UUID categoryId,

    @NotNull(message = "Transaction type is required")
    TransactionType type,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    String payee,
    String notes,

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    LocalDate date
) {}