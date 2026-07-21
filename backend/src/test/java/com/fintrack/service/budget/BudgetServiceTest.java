package com.fintrack.service.budget;

import com.fintrack.domain.model.Budget;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.request.CreateBudgetRequest;
import com.fintrack.web.dto.response.BudgetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(
            budgetRepository, categoryRepository, userRepository, transactionRepository
        );
    }

    private Category category(UUID id) {
        Category c = new Category();
        c.setId(id);
        c.setName("Groceries");
        c.setColor("#00ff00");
        return c;
    }

    private Budget budget(UUID id, UUID userId, Category category, LocalDate month, BigDecimal amount, boolean rollover) {
        User user = new User();
        user.setId(userId);

        Budget b = new Budget();
        b.setId(id);
        b.setUser(user);
        b.setCategory(category);
        b.setMonth(month);
        b.setAmount(amount);
        b.setRollover(rollover);
        return b;
    }

    // ── getBudgets ────────────────────────────────────────────────────────────

    @Test
    void getBudgets_returnsBudgetsForMonthWithLiveSpend() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate firstDay = LocalDate.of(2026, 7, 1);
        LocalDate lastDay = LocalDate.of(2026, 7, 31);

        Category cat = category(categoryId);
        Budget b = budget(UUID.randomUUID(), userId, cat, firstDay, new BigDecimal("200.00"), false);

        when(budgetRepository.findByUserIdAndMonth(userId, firstDay)).thenReturn(List.of(b));
        when(transactionRepository.sumExpensesByCategoryAndDateRange(userId, categoryId, firstDay, lastDay))
            .thenReturn(new BigDecimal("50.00"));

        List<BudgetResponse> result = budgetService.getBudgets(userId, firstDay);

        assertThat(result).hasSize(1);
        BudgetResponse res = result.get(0);
        assertThat(res.spentAmount()).isEqualByComparingTo("50.00");
        assertThat(res.remainingAmount()).isEqualByComparingTo("150.00");
        assertThat(res.percentUsed()).isEqualTo(25.0);
        assertThat(res.isOverBudget()).isFalse();
    }

    @Test
    void getBudgets_normalisesArbitraryDayToFirstOfMonth() {
        UUID userId = UUID.randomUUID();
        LocalDate arbitraryDay = LocalDate.of(2026, 7, 15);
        LocalDate expectedFirstDay = LocalDate.of(2026, 7, 1);

        when(budgetRepository.findByUserIdAndMonth(userId, expectedFirstDay)).thenReturn(List.of());

        budgetService.getBudgets(userId, arbitraryDay);

        verify(budgetRepository).findByUserIdAndMonth(userId, expectedFirstDay);
    }

    // ── createBudget ──────────────────────────────────────────────────────────

    @Test
    void createBudget_whenNoExistingBudget_savesAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 7, 1);

        User user = new User();
        user.setId(userId);
        Category cat = category(categoryId);

        CreateBudgetRequest req = new CreateBudgetRequest(
            categoryId, month, new BigDecimal("300.00"), true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonth(userId, categoryId, month))
            .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(transactionRepository.sumExpensesByCategoryAndDateRange(
            eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

        BudgetResponse res = budgetService.createBudget(userId, req);

        assertThat(res.budgetAmount()).isEqualByComparingTo("300.00");
        assertThat(res.rollover()).isTrue();
        assertThat(res.categoryId()).isEqualTo(categoryId);
        assertThat(res.month()).isEqualTo(month);
    }

    @Test
    void createBudget_whenUserNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        CreateBudgetRequest req = new CreateBudgetRequest(
            UUID.randomUUID(), LocalDate.of(2026, 7, 1), new BigDecimal("300.00"), false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> budgetService.createBudget(userId, req));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudget_whenCategoryNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        CreateBudgetRequest req = new CreateBudgetRequest(
            categoryId, LocalDate.of(2026, 7, 1), new BigDecimal("300.00"), false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> budgetService.createBudget(userId, req));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudget_whenBudgetAlreadyExistsForCategoryAndMonth_throwsBadRequestException() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 7, 1);

        User user = new User();
        user.setId(userId);
        Category cat = category(categoryId);
        Budget existing = budget(UUID.randomUUID(), userId, cat, month, new BigDecimal("100.00"), false);

        CreateBudgetRequest req = new CreateBudgetRequest(
            categoryId, month, new BigDecimal("300.00"), false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonth(userId, categoryId, month))
            .thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class,
            () -> budgetService.createBudget(userId, req));
        verify(budgetRepository, never()).save(any());
    }

    // ── updateBudget ──────────────────────────────────────────────────────────

    @Test
    void updateBudget_whenFound_updatesAmountAndRolloverAndSaves() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 7, 1);

        Category cat = category(categoryId);
        Budget existing = budget(budgetId, userId, cat, month, new BigDecimal("100.00"), false);

        CreateBudgetRequest req = new CreateBudgetRequest(
            categoryId, month, new BigDecimal("500.00"), true
        );

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.sumExpensesByCategoryAndDateRange(
            eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

        BudgetResponse res = budgetService.updateBudget(userId, budgetId, req);

        assertThat(res.budgetAmount()).isEqualByComparingTo("500.00");
        assertThat(res.rollover()).isTrue();
        assertThat(existing.getAmount()).isEqualByComparingTo("500.00");
        assertThat(existing.isRollover()).isTrue();
    }

    @Test
    void updateBudget_whenNotFoundOrNotOwnedByUser_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        CreateBudgetRequest req = new CreateBudgetRequest(
            UUID.randomUUID(), LocalDate.of(2026, 7, 1), new BigDecimal("500.00"), true
        );

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> budgetService.updateBudget(userId, budgetId, req));
        verify(budgetRepository, never()).save(any());
    }

    // ── deleteBudget ──────────────────────────────────────────────────────────

    @Test
    void deleteBudget_whenFound_deletesBudget() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget existing = budget(budgetId, userId, category(UUID.randomUUID()),
            LocalDate.of(2026, 7, 1), new BigDecimal("100.00"), false);

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(existing));

        budgetService.deleteBudget(userId, budgetId);

        verify(budgetRepository).delete(existing);
    }

    @Test
    void deleteBudget_whenNotFoundOrNotOwnedByUser_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> budgetService.deleteBudget(userId, budgetId));
        verify(budgetRepository, never()).delete(any());
    }

    // ── toResponse (spend calculation) ───────────────────────────────────────

    @Test
    void toResponse_whenSpendUnderBudget_computesRemainingAndPercentUsed() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Budget b = budget(UUID.randomUUID(), userId, category(categoryId), from, new BigDecimal("200.00"), false);

        when(transactionRepository.sumExpensesByCategoryAndDateRange(userId, categoryId, from, to))
            .thenReturn(new BigDecimal("50.00"));

        BudgetResponse res = budgetService.toResponse(b, from, to);

        assertThat(res.remainingAmount()).isEqualByComparingTo("150.00");
        assertThat(res.percentUsed()).isEqualTo(25.0);
        assertThat(res.isOverBudget()).isFalse();
    }

    @Test
    void toResponse_whenSpendExceedsBudget_marksOverBudgetAndCapsPercentAt100() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Budget b = budget(UUID.randomUUID(), userId, category(categoryId), from, new BigDecimal("200.00"), false);

        when(transactionRepository.sumExpensesByCategoryAndDateRange(userId, categoryId, from, to))
            .thenReturn(new BigDecimal("350.00"));

        BudgetResponse res = budgetService.toResponse(b, from, to);

        assertThat(res.remainingAmount()).isEqualByComparingTo("-150.00");
        assertThat(res.isOverBudget()).isTrue();
        assertThat(res.percentUsed()).isEqualTo(100.0);
    }

    @Test
    void toResponse_whenBudgetAmountIsZero_percentUsedIsZeroNotDivideByZero() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Budget b = budget(UUID.randomUUID(), userId, category(categoryId), from, BigDecimal.ZERO, false);

        when(transactionRepository.sumExpensesByCategoryAndDateRange(userId, categoryId, from, to))
            .thenReturn(BigDecimal.ZERO);

        BudgetResponse res = budgetService.toResponse(b, from, to);

        assertThat(res.percentUsed()).isEqualTo(0.0);
        assertThat(res.isOverBudget()).isFalse();
    }
}
