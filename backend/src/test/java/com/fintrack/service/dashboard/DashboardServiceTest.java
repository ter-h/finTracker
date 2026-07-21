package com.fintrack.service.dashboard;

import com.fintrack.domain.enums.AccountType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Budget;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.Transaction;
import com.fintrack.domain.model.User;
import com.fintrack.domain.enums.TransactionType;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.service.budget.BudgetService;
import com.fintrack.web.dto.response.BudgetResponse;
import com.fintrack.web.dto.response.DashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetService budgetService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
            accountRepository, transactionRepository, budgetRepository, budgetService
        );
    }

    private Account account(BigDecimal balance) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setName("Checking");
        a.setType(AccountType.CHECKING);
        a.setCurrency("AUD");
        a.setBalance(balance);
        return a;
    }

    @Test
    void getDashboard_computesNetWorthFromActiveAccounts() {
        UUID userId = UUID.randomUUID();

        when(accountRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of(account(new BigDecimal("100.00")), account(new BigDecimal("250.00"))));
        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(budgetRepository.findByUserIdAndMonth(any(), any())).thenReturn(List.of());
        when(transactionRepository.findTop10ByUserIdOrderByDateDescCreatedAtDesc(userId))
            .thenReturn(List.of());

        DashboardResponse res = dashboardService.getDashboard(userId);

        assertThat(res.netWorth()).isEqualByComparingTo("350.00");
        assertThat(res.accounts()).hasSize(2);
    }

    @Test
    void getDashboard_computesMonthlySavingsAsIncomeMinusExpenses() {
        UUID userId = UUID.randomUUID();

        when(accountRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of());
        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(new BigDecimal("400.00"));
        when(budgetRepository.findByUserIdAndMonth(any(), any())).thenReturn(List.of());
        when(transactionRepository.findTop10ByUserIdOrderByDateDescCreatedAtDesc(userId))
            .thenReturn(List.of());

        DashboardResponse res = dashboardService.getDashboard(userId);

        assertThat(res.monthlyIncome()).isEqualByComparingTo("1000.00");
        assertThat(res.monthlyExpenses()).isEqualByComparingTo("400.00");
        assertThat(res.monthlySavings()).isEqualByComparingTo("600.00");
    }

    @Test
    void getDashboard_mapsBudgetsUsingBudgetService() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Groceries");

        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUser(user);
        budget.setCategory(category);
        budget.setAmount(new BigDecimal("200.00"));

        BudgetResponse mappedResponse = new BudgetResponse(
            budget.getId(), category.getId(), "Groceries", null,
            LocalDate.now().withDayOfMonth(1), new BigDecimal("200.00"),
            new BigDecimal("50.00"), new BigDecimal("150.00"), 25.0, false, false
        );

        when(accountRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of());
        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(budgetRepository.findByUserIdAndMonth(any(), any())).thenReturn(List.of(budget));
        when(budgetService.toResponse(any(Budget.class), any(), any())).thenReturn(mappedResponse);
        when(transactionRepository.findTop10ByUserIdOrderByDateDescCreatedAtDesc(userId))
            .thenReturn(List.of());

        DashboardResponse res = dashboardService.getDashboard(userId);

        assertThat(res.budgets()).containsExactly(mappedResponse);
    }

    @Test
    void getDashboard_returnsUpToTenRecentTransactions() {
        UUID userId = UUID.randomUUID();

        Account account = account(BigDecimal.ZERO);
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccount(account);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("10.00"));
        t.setDate(LocalDate.now());

        when(accountRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of());
        when(transactionRepository.sumIncomeByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumExpensesByDateRange(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(budgetRepository.findByUserIdAndMonth(any(), any())).thenReturn(List.of());
        when(transactionRepository.findTop10ByUserIdOrderByDateDescCreatedAtDesc(userId))
            .thenReturn(List.of(t));

        DashboardResponse res = dashboardService.getDashboard(userId);

        assertThat(res.recentTransactions()).hasSize(1);
        assertThat(res.recentTransactions().get(0).id()).isEqualTo(t.getId());
    }
}
