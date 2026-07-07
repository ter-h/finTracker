package com.fintrack.web.dto.response;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserResponse user
) {
    // Factory method for convenience
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}