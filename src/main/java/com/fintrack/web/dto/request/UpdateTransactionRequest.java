package com.fintrack.web.dto.request;

import com.fintrack.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// All fields optional — only update what's provided
public record UpdateTransactionRequest(
    UUID accountId,
    UUID categoryId,
    TransactionType type,

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    String payee,
    String notes,
    LocalDate date
) {}