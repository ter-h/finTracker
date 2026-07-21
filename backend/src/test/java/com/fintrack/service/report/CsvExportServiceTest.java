package com.fintrack.service.report;

import com.fintrack.domain.enums.AccountType;
import com.fintrack.domain.enums.TransactionType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.Transaction;
import com.fintrack.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private CsvExportService csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService(transactionRepository);
    }

    private Transaction transaction(String payee, Category category, String notes) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setName("Checking");
        account.setType(AccountType.CHECKING);

        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccount(account);
        t.setCategory(category);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("42.50"));
        t.setPayee(payee);
        t.setNotes(notes);
        t.setDate(LocalDate.of(2026, 7, 1));
        return t;
    }

    @Test
    void exportTransactionsCsv_includesHeaderRow() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
            .thenReturn(List.of());

        byte[] csv = csvExportService.exportTransactionsCsv(userId, from, to);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).contains("Date,Payee,Category,Type,Amount,Account,Notes");
    }

    @Test
    void exportTransactionsCsv_writesRowForEachTransaction() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Category category = new Category();
        category.setName("Groceries");

        Transaction t = transaction("Whole Foods", category, "weekly shop");

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
            .thenReturn(List.of(t));

        byte[] csv = csvExportService.exportTransactionsCsv(userId, from, to);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).contains("Whole Foods");
        assertThat(content).contains("Groceries");
        assertThat(content).contains("EXPENSE");
        assertThat(content).contains("42.50");
        assertThat(content).contains("weekly shop");
    }

    @Test
    void exportTransactionsCsv_handlesNullCategoryPayeeAndNotes() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Transaction t = transaction(null, null, null);

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
            .thenReturn(List.of(t));

        byte[] csv = csvExportService.exportTransactionsCsv(userId, from, to);

        assertThat(csv).isNotEmpty();
    }
}
