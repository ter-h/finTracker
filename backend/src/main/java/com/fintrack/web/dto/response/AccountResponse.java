package com.fintrack.web.dto.response;

import com.fintrack.domain.model.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String name,
    String type,
    String currency,
    BigDecimal balance,
    boolean isArchived,
    Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getType().name(),
            account.getCurrency(),
            account.getBalance(),
            account.isArchived(),
            account.getCreatedAt()
        );
    }
}