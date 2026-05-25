package com.fintrack.service.transaction;

import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.Transaction;
import com.fintrack.domain.model.User;
import com.fintrack.domain.enums.TransactionType;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.request.CreateTransactionRequest;
import com.fintrack.web.dto.request.UpdateTransactionRequest;
import com.fintrack.web.dto.response.PageResponse;
import com.fintrack.web.dto.response.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;

    // ── List ──────────────────────────────────────────────────────────────────

    public PageResponse<TransactionResponse> listTransactions(
        UUID userId, int page, int size, UUID accountId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> result;

        if (accountId != null) {
            result = transactionRepository
                .findByUserIdAndAccountIdOrderByDateDescCreatedAtDesc(
                    userId, accountId, pageable
                );
        } else {
            result = transactionRepository
                .findByUserIdOrderByDateDescCreatedAtDesc(userId, pageable);
        }

        return PageResponse.from(result, TransactionResponse::from);
    }

    // ── Get single ────────────────────────────────────────────────────────────

    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        Transaction t = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return TransactionResponse.from(t);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse createTransaction(
        UUID userId, CreateTransactionRequest request
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify the account belongs to this user
        Account account = accountRepository
            .findByIdAndUserId(request.accountId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Category is optional
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Transaction t = new Transaction();
        t.setUser(user);
        t.setAccount(account);
        t.setCategory(category);
        t.setType(request.type());
        t.setAmount(request.amount());
        t.setPayee(request.payee());
        t.setNotes(request.notes());
        t.setDate(request.date());

        t = transactionRepository.save(t);

        // Update the account balance
        updateAccountBalance(account, request.type(), request.amount(), true);
        accountRepository.save(account);

        log.info("Transaction created: userId={} txId={} type={} amount={}",
            userId, t.getId(), t.getType(), t.getAmount());

        return TransactionResponse.from(t);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse updateTransaction(
        UUID userId, UUID transactionId, UpdateTransactionRequest request
    ) {
        Transaction t = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Reverse the old balance effect before applying the new one
        Account oldAccount = t.getAccount();
        updateAccountBalance(oldAccount, t.getType(), t.getAmount(), false);
        accountRepository.save(oldAccount);

        // Apply updates
        if (request.accountId() != null) {
            Account newAccount = accountRepository
                .findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            t.setAccount(newAccount);
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            t.setCategory(category);
        }
        if (request.type()   != null) t.setType(request.type());
        if (request.amount() != null) t.setAmount(request.amount());
        if (request.payee()  != null) t.setPayee(request.payee());
        if (request.notes()  != null) t.setNotes(request.notes());
        if (request.date()   != null) t.setDate(request.date());

        // Apply new balance effect
        updateAccountBalance(t.getAccount(), t.getType(), t.getAmount(), true);
        accountRepository.save(t.getAccount());

        t = transactionRepository.save(t);
        return TransactionResponse.from(t);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction t = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Reverse the balance effect before deleting
        updateAccountBalance(t.getAccount(), t.getType(), t.getAmount(), false);
        accountRepository.save(t.getAccount());

        transactionRepository.delete(t);
        log.info("Transaction deleted: userId={} txId={}", userId, transactionId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Adjust an account balance when a transaction is created (add=true)
     * or reversed/deleted (add=false).
     *
     * INCOME  → adds to balance
     * EXPENSE → subtracts from balance
     * TRANSFER → handled by caller (two separate transactions)
     */
    private void updateAccountBalance(
        Account account, TransactionType type, BigDecimal amount, boolean add
    ) {
        BigDecimal delta = add ? amount : amount.negate();

        switch (type) {
            case INCOME   -> account.setBalance(account.getBalance().add(delta));
            case EXPENSE  -> account.setBalance(account.getBalance().subtract(delta));
            case TRANSFER -> {} // transfer logic handled separately
        }
    }
}