package com.fintrack.service.dashboard;

import com.fintrack.domain.model.Transaction;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.service.budget.BudgetService;
import com.fintrack.web.dto.response.AccountResponse;
import com.fintrack.web.dto.response.BudgetResponse;
import com.fintrack.web.dto.response.DashboardResponse;
import com.fintrack.web.dto.response.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository      budgetRepository;
    private final BudgetService         budgetService;

    public DashboardResponse getDashboard(UUID userId) {
        LocalDate today     = LocalDate.now();
        LocalDate firstDay  = today.withDayOfMonth(1);
        LocalDate lastDay   = today.withDayOfMonth(today.lengthOfMonth());

        // 1. Net worth = sum of all active account balances
        List<AccountResponse> accounts = accountRepository
            .findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId)
            .stream()
            .map(AccountResponse::from)
            .toList();

        BigDecimal netWorth = accounts.stream()
            .map(AccountResponse::balance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Monthly income and expenses
        BigDecimal monthlyIncome = transactionRepository
            .sumIncomeByDateRange(userId, firstDay, lastDay);

        BigDecimal monthlyExpenses = transactionRepository
            .sumExpensesByDateRange(userId, firstDay, lastDay);

        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

        // 3. Budget status for this month
        List<BudgetResponse> budgets = budgetRepository
            .findByUserIdAndMonth(userId, firstDay)
            .stream()
            .map(b -> budgetService.toResponse(b, firstDay, lastDay))
            .toList();

        // 4. Recent transactions
        List<TransactionResponse> recent = transactionRepository
            .findTop10ByUserIdOrderByDateDescCreatedAtDesc(userId)
            .stream()
            .map(TransactionResponse::from)
            .toList();

        return new DashboardResponse(
            netWorth,
            monthlyIncome,
            monthlyExpenses,
            monthlySavings,
            budgets,
            accounts,
            recent
        );
    }
}