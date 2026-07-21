package com.fintrack.service.transaction;

import com.fintrack.domain.enums.AccountType;
import com.fintrack.domain.enums.TransactionType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.User;
import com.fintrack.domain.model.Transaction;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.response.ImportSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        csvImportService = new CsvImportService(
            transactionRepository, accountRepository, categoryRepository, userRepository
        );
    }

    private void stubUserAndAccount(UUID userId, UUID accountId, Account account) {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
    }

    private Account account(BigDecimal balance) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setName("Checking");
        a.setType(AccountType.CHECKING);
        a.setBalance(balance);
        return a;
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file", "transactions.csv", "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void importCsv_whenFileIsEmpty_throwsBadRequestException() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        MockMultipartFile empty = new MockMultipartFile("file", "transactions.csv", "text/csv", new byte[0]);

        assertThrows(BadRequestException.class,
            () -> csvImportService.importCsv(userId, accountId, empty));
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void importCsv_whenFileIsNotCsv_throwsBadRequestException() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        MockMultipartFile txt = new MockMultipartFile(
            "file", "transactions.txt", "text/plain", "date,amount\n".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(BadRequestException.class,
            () -> csvImportService.importCsv(userId, accountId, txt));
    }

    @Test
    void importCsv_whenFileTooLarge_throwsBadRequestException() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile big = new MockMultipartFile("file", "transactions.csv", "text/csv", tooBig);

        assertThrows(BadRequestException.class,
            () -> csvImportService.importCsv(userId, accountId, big));
    }

    @Test
    void importCsv_whenUserNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> csvImportService.importCsv(userId, accountId, csvFile("date,amount\n2026-07-01,10.00\n")));
    }

    @Test
    void importCsv_whenAccountNotOwnedByUser_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> csvImportService.importCsv(userId, accountId, csvFile("date,amount\n2026-07-01,10.00\n")));
    }

    // ── Parsing / import happy path ───────────────────────────────────────────

    @Test
    void importCsv_parsesNegativeAmountAsExpenseAndPositiveAsIncome() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,amount,payee\n"
            + "2026-07-01,-20.00,Coffee Shop\n"
            + "2026-07-02,50.00,Employer\n";

        ImportSummaryResponse summary = csvImportService.importCsv(userId, accountId, csvFile(csv));

        assertThat(summary.imported()).isEqualTo(2);
        assertThat(summary.failed()).isEqualTo(0);
        assertThat(summary.skipped()).isEqualTo(0);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();

        assertThat(saved).hasSize(2);
        Transaction expense = saved.stream().filter(t -> t.getType() == TransactionType.EXPENSE).findFirst().orElseThrow();
        assertThat(expense.getAmount()).isEqualByComparingTo("20.00");
        Transaction income = saved.stream().filter(t -> t.getType() == TransactionType.INCOME).findFirst().orElseThrow();
        assertThat(income.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void importCsv_updatesAccountBalanceByNetDelta() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,amount\n"
            + "2026-07-01,-20.00\n"   // expense
            + "2026-07-02,50.00\n";  // income

        csvImportService.importCsv(userId, accountId, csvFile(csv));

        // 100 - 20 + 50 = 130
        assertThat(acc.getBalance()).isEqualByComparingTo("130.00");
        verify(accountRepository).save(acc);
    }

    @Test
    void importCsv_skipsDuplicateRowsAlreadyImported() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());
        when(transactionRepository.existsByUserIdAndImportHash(any(), any())).thenReturn(true);

        String csv = "date,amount\n2026-07-01,-20.00\n";

        ImportSummaryResponse summary = csvImportService.importCsv(userId, accountId, csvFile(csv));

        assertThat(summary.skipped()).isEqualTo(1);
        assertThat(summary.imported()).isEqualTo(0);
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void importCsv_rowMissingDateOrAmount_countsAsFailed() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,payee\n2026-07-01,Some Payee\n"; // no amount column

        ImportSummaryResponse summary = csvImportService.importCsv(userId, accountId, csvFile(csv));

        assertThat(summary.failed()).isEqualTo(1);
        assertThat(summary.imported()).isEqualTo(0);
    }

    @Test
    void importCsv_unparseableDate_countsAsFailed() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,amount\nnot-a-date,-20.00\n";

        ImportSummaryResponse summary = csvImportService.importCsv(userId, accountId, csvFile(csv));

        assertThat(summary.failed()).isEqualTo(1);
    }

    @Test
    void importCsv_alternateDateFormat_parsesSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,amount\n07/15/2026,-20.00\n"; // MM/dd/yyyy

        ImportSummaryResponse summary = csvImportService.importCsv(userId, accountId, csvFile(csv));

        assertThat(summary.imported()).isEqualTo(1);
    }

    @Test
    void importCsv_amountWithCurrencySymbolsAndCommas_parsesCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,amount\n2026-07-01,\"-$1,234.56\"\n";

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        csvImportService.importCsv(userId, accountId, csvFile(csv));
        verify(transactionRepository).saveAll(captor.capture());

        assertThat(captor.getValue().get(0).getAmount()).isEqualByComparingTo("1234.56");
        assertThat(captor.getValue().get(0).getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void importCsv_autoCategorisesBasedOnPayeeKeyword() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);

        Category groceries = new Category();
        groceries.setId(UUID.randomUUID());
        groceries.setName("Food & Dining");
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of(groceries));

        String csv = "date,amount,payee\n2026-07-01,-20.00,Local Pizza Restaurant\n";

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        csvImportService.importCsv(userId, accountId, csvFile(csv));
        verify(transactionRepository).saveAll(captor.capture());

        assertThat(captor.getValue().get(0).getCategory()).isEqualTo(groceries);
    }

    @Test
    void importCsv_payeeWithNoKeywordMatch_leavesCategoryNull() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);

        Category groceries = new Category();
        groceries.setId(UUID.randomUUID());
        groceries.setName("Food & Dining");
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of(groceries));

        String csv = "date,amount,payee\n2026-07-01,-20.00,Some Random Payee\n";

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        csvImportService.importCsv(userId, accountId, csvFile(csv));
        verify(transactionRepository).saveAll(captor.capture());

        assertThat(captor.getValue().get(0).getCategory()).isNull();
    }

    @Test
    void importCsv_whenAllRowsAreDuplicatesOrFailed_doesNotCallSaveAllOrUpdateBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account acc = account(new BigDecimal("100.00"));
        stubUserAndAccount(userId, accountId, acc);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        String csv = "date,payee\n2026-07-01,No Amount Column\n"; // will fail to parse (no amount)

        csvImportService.importCsv(userId, accountId, csvFile(csv));

        verify(transactionRepository, never()).saveAll(any());
        verify(accountRepository, never()).save(any());
        assertThat(acc.getBalance()).isEqualByComparingTo("100.00");
    }
}
