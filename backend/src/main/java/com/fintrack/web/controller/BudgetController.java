package com.fintrack.web.controller;

import com.fintrack.security.UserPrincipal;
import com.fintrack.service.budget.BudgetService;
import com.fintrack.web.dto.request.CreateBudgetRequest;
import com.fintrack.web.dto.response.BudgetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // GET /api/v1/budgets?month=2024-06-01
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ResponseEntity.ok(budgetService.getBudgets(principal.getId(), month));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateBudgetRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(budgetService.createBudget(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody CreateBudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.updateBudget(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        budgetService.deleteBudget(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
