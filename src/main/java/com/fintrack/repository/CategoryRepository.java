package com.fintrack.repository;

import com.fintrack.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Get system categories + user's own custom categories
    @Query("SELECT c FROM Category c WHERE c.isSystem = true OR c.user.id = :userId ORDER BY c.name ASC")
    List<Category> findAvailableForUser(UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}