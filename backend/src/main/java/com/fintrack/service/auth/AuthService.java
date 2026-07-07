package com.fintrack.service.auth;

import com.fintrack.domain.model.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.UnauthorisedException;
import com.fintrack.repository.UserRepository;
import com.fintrack.security.JwtService;
import com.fintrack.web.dto.request.LoginRequest;
import com.fintrack.web.dto.request.RegisterRequest;
import com.fintrack.web.dto.response.AuthResponse;
import com.fintrack.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Slf4j gives us a log variable for free (log.info, log.error etc.)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check email is not already taken
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        // 2. Create the user
        User user = new User();
        user.setEmail(request.email().toLowerCase().strip());
        user.setDisplayName(request.displayName().strip());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // 3. Save to database
        user = userRepository.save(user);

        log.info("New user registered: userId={}", user.getId());

        // 4. Generate JWT and return
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Find user by email
        User user = userRepository
            .findByEmailAndDeletedAtIsNull(request.email().toLowerCase().strip())
            .orElseThrow(() -> new UnauthorisedException("Invalid email or password"));

        // 2. Check password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorisedException("Invalid email or password");
        }

        log.info("User logged in: userId={}", user.getId());

        // 3. Generate JWT and return
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, UserResponse.from(user));
    }
}