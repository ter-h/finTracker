package com.fintrack.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
    UUID       id,
    UUID       categoryId,
    String     categoryName,
    String     categoryColor,
    LocalDate  month,
    BigDecimal budgetAmount,
    BigDecimal spentAmount,
    BigDecimal remainingAmount,
    double     percentUsed,     // 0–100
    boolean    isOverBudget,
    boolean    rollover
) {}