package com.fintrack.repository;

import com.fintrack.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    // Get all accounts for a user (not archived)
    List<Account> findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(UUID userId);

    // Find a specific account that belongs to a specific user (security check)
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}