package com.fintrack.domain.model;

import com.fintrack.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Always stored as positive — type determines if it adds or subtracts
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private String payee;
    private String notes;

    @Column(nullable = false)
    private LocalDate date;

    // Links two transactions that form a transfer
    private UUID transferPairId;

    // SHA-256 hash of raw CSV row — used to detect duplicate imports
    private String importHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

}