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
class PdfExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private PdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        pdfExportService = new PdfExportService(transactionRepository);
    }

    private Transaction transaction(TransactionType type) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setName("Checking");
        account.setType(AccountType.CHECKING);

        Category category = new Category();
        category.setName("Groceries");

        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccount(account);
        t.setCategory(category);
        t.setType(type);
        t.setAmount(new BigDecimal("42.50"));
        t.setPayee("Whole Foods");
        t.setDate(LocalDate.of(2026, 7, 1));
        return t;
    }

    @Test
    void exportTransactionsPdf_withNoTransactions_producesValidPdfBytes() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
            .thenReturn(List.of());

        byte[] pdf = pdfExportService.exportTransactionsPdf(userId, from, to);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void exportTransactionsPdf_withIncomeAndExpenseTransactions_producesValidPdfBytes() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to))
            .thenReturn(List.of(transaction(TransactionType.INCOME), transaction(TransactionType.EXPENSE)));

        byte[] pdf = pdfExportService.exportTransactionsPdf(userId, from, to);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
