package com.fintrack.web.controller;

import com.fintrack.domain.model.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.UserRepository;
import com.fintrack.security.UserPrincipal;
import com.fintrack.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // DELETE /api/v1/users/me
    // Soft-deletes the account — all data is retained for 30 days,
    // then a scheduled job can purge it (GDPR compliant)
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteMe(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setDeletedAt(Instant.now());
        // Anonymise the email so it can be re-used if they want to sign up again
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }
}