package com.fintrack.repository;

import com.fintrack.domain.model.BudgetAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BudgetAlertLogRepository extends JpaRepository<BudgetAlertLog, UUID> {

    // Check if we already sent this alert this month
    boolean existsByBudgetIdAndThresholdAndMonth(
        UUID budgetId, int threshold, LocalDate month
    );
}