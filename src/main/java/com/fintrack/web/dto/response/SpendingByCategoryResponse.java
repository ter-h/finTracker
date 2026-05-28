package com.fintrack.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpendingByCategoryResponse(
    List<CategorySpend> categories,
    BigDecimal          totalExpenses,
    String              from,
    String              to
) {
    public record CategorySpend(
        UUID       categoryId,
        String     categoryName,
        String     categoryColor,
        BigDecimal amount,
        double     percentage    // share of total spending
    ) {}
}