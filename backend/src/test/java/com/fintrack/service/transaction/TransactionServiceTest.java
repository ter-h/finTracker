package com.fintrack.service.transaction;

import com.fintrack.domain.enums.TransactionType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Transaction;
import com.fintrack.domain.model.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.request.CreateTransactionRequest;
import com.fintrack.web.dto.request.UpdateTransactionRequest;
import com.fintrack.web.dto.response.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
            transactionRepository, accountRepository, categoryRepository, userRepository
        );
    }

    @Test
    void createTransaction_income_addsToAccountBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setBalance(new BigDecimal("100.00"));

        CreateTransactionRequest req = new CreateTransactionRequest(
            accountId, null, TransactionType.INCOME,
            new BigDecimal("50.00"), "Employer", null, LocalDate.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        transactionService.createTransaction(userId, req);

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(account);
    }

    @Test
    void createTransaction_expense_subtractsFromAccountBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setBalance(new BigDecimal("100.00"));

        CreateTransactionRequest req = new CreateTransactionRequest(
            accountId, null, TransactionType.EXPENSE,
            new BigDecimal("30.00"), "Groceries", null, LocalDate.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        transactionService.createTransaction(userId, req);

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void createTransaction_whenAccountNotOwnedByUser_throwsAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        CreateTransactionRequest req = new CreateTransactionRequest(
            accountId, null, TransactionType.EXPENSE,
            new BigDecimal("30.00"), "Groceries", null, LocalDate.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> transactionService.createTransaction(userId, req));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getTransaction_whenFound_returnsMappedResponse() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        Account account = new Account();
        account.setId(UUID.randomUUID());

        Transaction t = new Transaction();
        t.setId(txId);
        t.setAccount(account);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("20.00"));
        t.setDate(LocalDate.now());

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(t));

        TransactionResponse res = transactionService.getTransaction(userId, txId);

        assertThat(res.id()).isEqualTo(txId);
    }

    @Test
    void getTransaction_whenNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> transactionService.getTransaction(userId, txId));
    }

    @Test
    void updateTransaction_changingAmount_reversesOldEffectAndAppliesNew() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setBalance(new BigDecimal("100.00"));

        Transaction t = new Transaction();
        t.setId(txId);
        t.setAccount(account);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("20.00"));
        t.setDate(LocalDate.now());

        UpdateTransactionRequest req = new UpdateTransactionRequest(
            null, null, null, new BigDecimal("50.00"), null, null, null
        );

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(t));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.updateTransaction(userId, txId, req);

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void deleteTransaction_reversesBalanceEffectAndDeletes() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setBalance(new BigDecimal("100.00"));

        Transaction t = new Transaction();
        t.setId(txId);
        t.setAccount(account);
        t.setType(TransactionType.INCOME);
        t.setAmount(new BigDecimal("40.00"));
        t.setDate(LocalDate.now());

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(userId, txId);

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        verify(transactionRepository).delete(t);
    }

    @Test
    void deleteTransaction_whenNotFound_throwsAndDoesNotDelete() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> transactionService.deleteTransaction(userId, txId));
        verify(transactionRepository, never()).delete(any());
    }
}
