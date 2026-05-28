package com.fintrack.repository;

import com.fintrack.domain.model.NotificationPrefs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPrefsRepository extends JpaRepository<NotificationPrefs, UUID> {
    Optional<NotificationPrefs> findByUserId(UUID userId);
}