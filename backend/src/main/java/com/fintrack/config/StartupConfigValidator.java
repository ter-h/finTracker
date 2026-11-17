package com.fintrack.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigValidator {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @PostConstruct
    public void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "APP_JWT_SECRET is required. Set it in your .env or environment before starting the app."
            );
        }
    }
}
