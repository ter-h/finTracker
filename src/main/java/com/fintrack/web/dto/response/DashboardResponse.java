package com.fintrack.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(

    // Sum of all account balances
    BigDecimal netWorth,

    // Current calendar month totals
    BigDecimal monthlyIncome,
    BigDecimal monthlyExpenses,
    BigDecimal monthlySavings,      // income - expenses

    // Budget status for this month (each category with a budget)
    List<BudgetResponse> budgets,

    // Account balances at a glance
    List<AccountResponse> accounts,

    // The 10 most recent transactions
    List<TransactionResponse> recentTransactions
) {}