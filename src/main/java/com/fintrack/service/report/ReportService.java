package com.fintrack.service.report;

import com.fintrack.repository.TransactionRepository;
import com.fintrack.web.dto.response.IncomeExpenseTrendResponse;
import com.fintrack.web.dto.response.IncomeExpenseTrendResponse.MonthPoint;
import com.fintrack.web.dto.response.SpendingByCategoryResponse;
import com.fintrack.web.dto.response.SpendingByCategoryResponse.CategorySpend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    // ── Spending by category ───────────────────────────────────────────────────

    public SpendingByCategoryResponse spendingByCategory(
        UUID userId, LocalDate from, LocalDate to
    ) {
        List<Object[]> rows = transactionRepository
            .sumExpensesGroupedByCategory(userId, from, to);

        // First pass: calculate total so we can compute percentages
        BigDecimal total = rows.stream()
            .map(r -> (BigDecimal) r[3])
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpend> categories = rows.stream().map(r -> {
            UUID       catId    = (UUID)       r[0];
            String     catName  = (String)     r[1];
            String     catColor = (String)     r[2];
            BigDecimal amount   = (BigDecimal) r[3];

            double pct = total.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                amount.divide(total, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100))
                      .doubleValue();

            return new CategorySpend(catId, catName, catColor, amount, pct);
        }).toList();

        return new SpendingByCategoryResponse(
            categories,
            total,
            from.toString(),
            to.toString()
        );
    }

    // ── Income vs expense trend ────────────────────────────────────────────────

    public IncomeExpenseTrendResponse incomeVsExpenseTrend(UUID userId, int months) {
        List<MonthPoint> points = new ArrayList<>();
        DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("yyyy-MM");

        // Walk backwards from current month
        for (int i = months - 1; i >= 0; i--) {
            YearMonth  ym       = YearMonth.now().minusMonths(i);
            LocalDate  from     = ym.atDay(1);
            LocalDate  to       = ym.atEndOfMonth();

            BigDecimal income   = transactionRepository
                .sumIncomeByDateRange(userId, from, to);
            BigDecimal expenses = transactionRepository
                .sumExpensesByDateRange(userId, from, to);
            BigDecimal savings  = income.subtract(expenses);

            points.add(new MonthPoint(ym.format(fmt), income, expenses, savings));
        }

        return new IncomeExpenseTrendResponse(points);
    }
}