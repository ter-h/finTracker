package com.fintrack.service.report;

import com.fintrack.repository.TransactionRepository;
import com.fintrack.web.dto.response.IncomeExpenseTrendResponse;
import com.fintrack.web.dto.response.SpendingByCategoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(transactionRepository);
    }

    // ── spendingByCategory ───────────────────────────────────────────────────

    @Test
    void spendingByCategory_computesPercentageOfTotal() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        UUID cat1 = UUID.randomUUID();
        UUID cat2 = UUID.randomUUID();
        List<Object[]> rows = List.of(
            new Object[]{cat1, "Groceries", "#00ff00", new BigDecimal("75.00")},
            new Object[]{cat2, "Transport", "#0000ff", new BigDecimal("25.00")}
        );

        when(transactionRepository.sumExpensesGroupedByCategory(userId, from, to))
            .thenReturn(rows);

        SpendingByCategoryResponse res = reportService.spendingByCategory(userId, from, to);

        assertThat(res.totalExpenses()).isEqualByComparingTo("100.00");
        assertThat(res.categories()).hasSize(2);
        assertThat(res.categories().get(0).percentage()).isEqualTo(75.0);
        assertThat(res.categories().get(1).percentage()).isEqualTo(25.0);
        assertThat(res.from()).isEqualTo(from.toString());
        assertThat(res.to()).isEqualTo(to.toString());
    }

    @Test
    void spendingByCategory_whenNoExpenses_returnsZeroPercentagesNotDivideByZero() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        when(transactionRepository.sumExpensesGroupedByCategory(userId, from, to))
            .thenReturn(List.of());

        SpendingByCategoryResponse res = reportService.spendingByCategory(userId, from, to);

        assertThat(res.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(res.categories()).isEmpty();
    }

    // ── incomeVsExpenseTrend ─────────────────────────────────────────────────

    @Test
    void incomeVsExpenseTrend_returnsRequestedNumberOfMonthPoints() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("300.00"));

        IncomeExpenseTrendResponse res = reportService.incomeVsExpenseTrend(userId, 3);

        assertThat(res.months()).hasSize(3);
        assertThat(res.months().get(2).month())
            .isEqualTo(YearMonth.now().toString());
    }

    @Test
    void incomeVsExpenseTrend_computesSavingsAsIncomeMinusExpenses() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("300.00"));

        IncomeExpenseTrendResponse res = reportService.incomeVsExpenseTrend(userId, 1);

        var point = res.months().get(0);
        assertThat(point.income()).isEqualByComparingTo("500.00");
        assertThat(point.expenses()).isEqualByComparingTo("300.00");
        assertThat(point.savings()).isEqualByComparingTo("200.00");
    }
}
