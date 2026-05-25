package com.fintrack.repository;

import com.fintrack.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Basic paginated list — used on the main transactions screen
    Page<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(
        UUID userId, Pageable pageable
    );

    // Filtered by account
    Page<Transaction> findByUserIdAndAccountIdOrderByDateDescCreatedAtDesc(
        UUID userId, UUID accountId, Pageable pageable
    );

    // Security: only fetch if it belongs to this user
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    // Dedup check for CSV imports
    boolean existsByUserIdAndImportHash(UUID userId, String importHash);

    // Sum expenses for a category in a date range — used by budget calculations
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id    = :userId
          AND t.category.id = :categoryId
          AND t.type        = com.fintrack.domain.enums.TransactionType.EXPENSE
          AND t.date       >= :from
          AND t.date       <= :to
    """)
    BigDecimal sumExpensesByCategoryAndDateRange(
        UUID userId, UUID categoryId, LocalDate from, LocalDate to
    );

    // Total income for a user in a date range — used by dashboard + reports
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type    = com.fintrack.domain.enums.TransactionType.INCOME
          AND t.date   >= :from
          AND t.date   <= :to
    """)
    BigDecimal sumIncomeByDateRange(UUID userId, LocalDate from, LocalDate to);

    // Total expenses for a user in a date range — used by dashboard + reports
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type    = com.fintrack.domain.enums.TransactionType.EXPENSE
          AND t.date   >= :from
          AND t.date   <= :to
    """)
    BigDecimal sumExpensesByDateRange(UUID userId, LocalDate from, LocalDate to);

    // Spending grouped by category — used by the reports screen
    @Query("""
        SELECT t.category.id, t.category.name, t.category.color,
               COALESCE(SUM(t.amount), 0) as total
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type    = com.fintrack.domain.enums.TransactionType.EXPENSE
          AND t.date   >= :from
          AND t.date   <= :to
          AND t.category IS NOT NULL
        GROUP BY t.category.id, t.category.name, t.category.color
        ORDER BY total DESC
    """)
    List<Object[]> sumExpensesGroupedByCategory(
        UUID userId, LocalDate from, LocalDate to
    );

    // Recent transactions for dashboard (last 10)
    List<Transaction> findTop10ByUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    // All transactions in a date range — used for CSV/PDF export
    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(
        UUID userId, LocalDate from, LocalDate to
    );
}