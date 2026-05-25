package com.fintrack.web.dto.response;

import com.fintrack.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID accountId,
    String accountName,
    UUID categoryId,
    String categoryName,
    String categoryColor,
    String type,
    BigDecimal amount,
    String payee,
    String notes,
    LocalDate date,
    Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
            t.getId(),
            t.getAccount().getId(),
            t.getAccount().getName(),
            t.getCategory() != null ? t.getCategory().getId()    : null,
            t.getCategory() != null ? t.getCategory().getName()  : null,
            t.getCategory() != null ? t.getCategory().getColor() : null,
            t.getType().name(),
            t.getAmount(),
            t.getPayee(),
            t.getNotes(),
            t.getDate(),
            t.getCreatedAt()
        );
    }
}