package com.fintrack.web.controller;

import com.fintrack.service.transaction.CsvImportService;
import com.fintrack.web.dto.response.ImportSummaryResponse;
import org.springframework.web.multipart.MultipartFile;
import com.fintrack.security.UserPrincipal;
import com.fintrack.service.transaction.TransactionService;
import com.fintrack.web.dto.request.CreateTransactionRequest;
import com.fintrack.web.dto.request.UpdateTransactionRequest;
import com.fintrack.web.dto.response.PageResponse;
import com.fintrack.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CsvImportService csvImportService;
    // GET /api/v1/transactions?page=0&size=20&accountId=xxx
    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0")  int  page,
        @RequestParam(defaultValue = "20") int  size,
        @RequestParam(required = false)    UUID accountId
    ) {
        return ResponseEntity.ok(
            transactionService.listTransactions(principal.getId(), page, size, accountId)
        );
    }

    // GET /api/v1/transactions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> get(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
            transactionService.getTransaction(principal.getId(), id)
        );
    }

    // POST /api/v1/transactions
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateTransactionRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(transactionService.createTransaction(principal.getId(), request));
    }

    // PUT /api/v1/transactions/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTransactionRequest request
    ) {
        return ResponseEntity.ok(
            transactionService.updateTransaction(principal.getId(), id, request)
        );
    }

    // DELETE /api/v1/transactions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        transactionService.deleteTransaction(principal.getId(), id);
        return ResponseEntity.noContent().build();   // 204 No Content
    }

    @PostMapping("/import")
    public ResponseEntity<ImportSummaryResponse> importCsv(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam UUID accountId,
        @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(
            csvImportService.importCsv(principal.getId(), accountId, file)
        );
    }
}