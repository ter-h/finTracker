package com.fintrack.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// record = immutable DTO, Java 16+. Much cleaner than a full class.
public record RegisterRequest(
    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be 2–100 characters")
    String displayName
) {}