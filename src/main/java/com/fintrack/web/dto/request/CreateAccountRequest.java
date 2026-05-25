package com.fintrack.web.dto.request;

import com.fintrack.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
    @NotBlank(message = "Account name is required")
    String name,

    @NotNull(message = "Account type is required")
    AccountType type,

    @NotBlank(message = "Currency is required")
    String currency,

    BigDecimal openingBalance  // optional, defaults to 0
) {}