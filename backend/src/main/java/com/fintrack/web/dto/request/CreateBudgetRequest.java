package com.fintrack.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBudgetRequest(

    @NotNull(message = "Category is required")
    UUID categoryId,

    // First day of the month: 2024-06-01
    @NotNull(message = "Month is required")
    LocalDate month,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Budget amount must be greater than zero")
    BigDecimal amount,

    boolean rollover   // carry unused budget to next month?
) {}