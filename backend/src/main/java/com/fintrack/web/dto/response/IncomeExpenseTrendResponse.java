package com.fintrack.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record IncomeExpenseTrendResponse(List<MonthPoint> months) {

    public record MonthPoint(
        String     month,       // "2024-06"
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal savings      // income - expenses
    ) {}
}