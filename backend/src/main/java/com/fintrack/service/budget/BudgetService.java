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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository      budgetRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;
    private final TransactionRepository transactionRepository;

    // Get all budgets for a month, with live "spent so far" figures
    public List<BudgetResponse> getBudgets(UUID userId, LocalDate month) {
        // Normalise to first day of the month
        LocalDate firstDay = month.withDayOfMonth(1);
        LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        return budgetRepository.findByUserIdAndMonth(userId, firstDay)
            .stream()
            .map(b -> toResponse(b, firstDay, lastDay))
            .toList();
    }

    @Transactional
    public BudgetResponse createBudget(UUID userId, CreateBudgetRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        LocalDate firstDay = request.month().withDayOfMonth(1);

        // One budget per category per month
        if (budgetRepository.findByUserIdAndCategoryIdAndMonth(
                userId, category.getId(), firstDay).isPresent()) {
            throw new BadRequestException(
                "A budget for this category and month already exists"
            );
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(firstDay);
        budget.setAmount(request.amount());
        budget.setRollover(request.rollover());

        budget = budgetRepository.save(budget);

        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        return toResponse(budget, firstDay, lastDay);
    }

    @Transactional
    public BudgetResponse updateBudget(
        UUID userId, UUID budgetId, CreateBudgetRequest request
    ) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        budget.setAmount(request.amount());
        budget.setRollover(request.rollover());
        budget = budgetRepository.save(budget);

        LocalDate firstDay = budget.getMonth();
        LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        return toResponse(budget, firstDay, lastDay);
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    // Build the full BudgetResponse with live spending data
    public BudgetResponse toResponse(Budget b, LocalDate from, LocalDate to) {
        BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
            b.getUser().getId(), b.getCategory().getId(), from, to
        );

        BigDecimal remaining = b.getAmount().subtract(spent);
        boolean overBudget   = spent.compareTo(b.getAmount()) > 0;

        double pct = b.getAmount().compareTo(BigDecimal.ZERO) == 0
            ? 0
            : spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100))
                   .doubleValue();

        return new BudgetResponse(
            b.getId(),
            b.getCategory().getId(),
            b.getCategory().getName(),
            b.getCategory().getColor(),
            b.getMonth(),
            b.getAmount(),
            spent,
            remaining,
            Math.min(pct, 100.0),   // cap at 100 for the UI bar
            overBudget,
            b.isRollover()
        );
    }
}