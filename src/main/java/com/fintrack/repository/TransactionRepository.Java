package com.fintrack.repository;

import com.fintrack.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Paginated list for a user — used on the main transactions screen
    Page<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    // Check for duplicate imports using the hash
    boolean existsByUserIdAndImportHash(UUID userId, String importHash);

    // Security check when editing/deleting
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    // Used for report: total spent in a category for a date range
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.category.id = :categoryId
          AND t.type = 'EXPENSE'
          AND t.date >= :from
          AND t.date <= :to
    """)
    BigDecimal sumExpensesByCategoryAndDateRange(
        UUID userId, UUID categoryId, LocalDate from, LocalDate to
    );
}