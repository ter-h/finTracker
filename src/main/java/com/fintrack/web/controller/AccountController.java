package com.fintrack.web.controller;

import com.fintrack.security.UserPrincipal;
import com.fintrack.service.account.AccountService;
import com.fintrack.web.dto.request.CreateAccountRequest;
import com.fintrack.web.dto.response.AccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // @AuthenticationPrincipal pulls the logged-in user out of the JWT filter
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getAccountsForUser(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateAccountRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(accountService.createAccount(principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse> archiveAccount(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(accountService.archiveAccount(principal.getId(), id));
    }
}