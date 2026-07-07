package com.fintrack.scheduler;

import com.fintrack.domain.model.Budget;
import com.fintrack.domain.model.BudgetAlertLog;
import com.fintrack.domain.model.NotificationPrefs;
import com.fintrack.repository.BudgetAlertLogRepository;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.NotificationPrefsRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertScheduler {

    private final BudgetRepository           budgetRepository;
    private final TransactionRepository      transactionRepository;
    private final BudgetAlertLogRepository   alertLogRepository;
    private final NotificationPrefsRepository prefsRepository;
    private final EmailService               emailService;

    // Runs every hour — "0 0 * * * *" = at second 0, minute 0, every hour
    @Scheduled(cron = "0 0 * * * *")
    public void checkBudgetAlerts() {
        LocalDate today    = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay  = today.withDayOfMonth(today.lengthOfMonth());

        // Load all budgets for this month in pages to avoid memory issues
        // at scale. For now a simple findAll per month is fine.
        List<Budget> budgets = budgetRepository.findAll()
            .stream()
            .filter(b -> b.getMonth().equals(firstDay))
            .toList();

        log.debug("Budget alert check: checking {} budgets", budgets.size());

        for (Budget budget : budgets) {
            try {
                checkBudget(budget, firstDay, lastDay);
            } catch (Exception e) {
                log.error("Error checking budget {}: {}", budget.getId(), e.getMessage());
            }
        }
    }

    private void checkBudget(Budget budget, LocalDate from, LocalDate to) {
        BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
            budget.getUser().getId(),
            budget.getCategory().getId(),
            from, to
        );

        if (budget.getAmount().compareTo(BigDecimal.ZERO) == 0) return;

        double pct = spent
            .divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();

        // Get user's notification preferences
        NotificationPrefs prefs = prefsRepository
            .findByUserId(budget.getUser().getId())
            .orElse(null);

        if (prefs == null) return;

        String email = prefs.getAlertEmail() != null
            ? prefs.getAlertEmail()
            : budget.getUser().getEmail();

        String name = budget.getUser().getDisplayName();

        // Check 100% threshold
        if (pct >= 100 && prefs.isBudgetAlert100()) {
            sendAlertIfNotAlreadySent(budget, 100, from, email, name, spent);
        }
        // Check 80% threshold (only if not already over 100%)
        else if (pct >= 80 && prefs.isBudgetAlert80()) {
            sendAlertIfNotAlreadySent(budget, 80, from, email, name, spent);
        }
    }

    private void sendAlertIfNotAlreadySent(
        Budget budget, int threshold, LocalDate month,
        String email, String displayName, BigDecimal spent
    ) {
        // Check if we already sent this alert this month
        boolean alreadySent = alertLogRepository
            .existsByBudgetIdAndThresholdAndMonth(budget.getId(), threshold, month);

        if (alreadySent) return;

        // Send the email
        emailService.sendBudgetAlert(
            email,
            displayName,
            budget.getCategory().getName(),
            budget.getAmount(),
            spent,
            threshold
        );

        // Record that we sent it
        BudgetAlertLog log = new BudgetAlertLog();
        log.setUser(budget.getUser());
        log.setBudget(budget);
        log.setThreshold(threshold);
        log.setMonth(month);
        alertLogRepository.save(log);
    }
}