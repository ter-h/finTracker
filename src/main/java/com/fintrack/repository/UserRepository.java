package com.fintrack.repository;

import com.fintrack.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring generates the SQL: SELECT * FROM users WHERE email = ? AND deleted_at IS NULL
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    // Check if email already exists
    boolean existsByEmail(String email);

    // For Google OAuth login
    Optional<User> findByGoogleId(String googleId);
}