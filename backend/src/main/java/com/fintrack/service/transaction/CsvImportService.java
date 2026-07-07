package com.fintrack.service.transaction;

import com.fintrack.domain.enums.TransactionType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.Category;
import com.fintrack.domain.model.Transaction;
import com.fintrack.domain.model.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.response.ImportSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;

    // Date formats we try to parse — covers most bank export formats
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Transactional
    public ImportSummaryResponse importCsv(
        UUID userId, UUID accountId, MultipartFile file
    ) {
        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only CSV files are supported");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new BadRequestException("File too large. Maximum size is 5MB");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<Category> categories = categoryRepository.findAvailableForUser(userId);

        int totalRows = 0, imported = 0, skipped = 0, failed = 0;
        List<Transaction> toSave = new ArrayList<>();

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
            );
            CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                totalRows++;
                try {
                    // Compute a hash of the raw row for dedup detection
                    String rawRow   = record.toString();
                    String rowHash  = sha256(rawRow);

                    // Skip if we've already imported this row
                    if (transactionRepository.existsByUserIdAndImportHash(userId, rowHash)) {
                        skipped++;
                        continue;
                    }

                    Transaction t = parseRecord(record, user, account, categories, rowHash);
                    if (t != null) {
                        toSave.add(t);
                        imported++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse CSV row {}: {}", totalRows, e.getMessage());
                    failed++;
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Could not read CSV file: " + e.getMessage());
        }

        // Bulk save all valid transactions
        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);

            // Recalculate account balance from all imports
            BigDecimal balanceDelta = toSave.stream()
                .map(t -> t.getType() == TransactionType.EXPENSE
                    ? t.getAmount().negate()
                    : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            account.setBalance(account.getBalance().add(balanceDelta));
            accountRepository.save(account);
        }

        log.info("CSV import complete: userId={} total={} imported={} skipped={} failed={}",
            userId, totalRows, imported, skipped, failed);

        return new ImportSummaryResponse(
            totalRows, imported, skipped, failed,
            String.format("Import complete: %d imported, %d duplicates skipped, %d failed",
                imported, skipped, failed)
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Transaction parseRecord(
        CSVRecord record,
        User user,
        Account account,
        List<Category> categories,
        String rowHash
    ) {
        // Try common column name variations from different banks
        String dateStr   = getField(record, "date", "transaction date", "posted date", "value date");
        String amountStr = getField(record, "amount", "debit", "credit", "transaction amount");
        String payee     = getField(record, "payee", "description", "merchant", "details", "memo");

        if (dateStr == null || amountStr == null) {
            return null; // can't parse without date and amount
        }

        LocalDate date = parseDate(dateStr);
        if (date == null) return null;

        // Clean the amount string: remove $, commas, spaces
        amountStr = amountStr.replaceAll("[^0-9.\\-]", "");
        if (amountStr.isEmpty()) return null;

        BigDecimal rawAmount = new BigDecimal(amountStr);

        // Negative amount = expense, positive = income (common bank format)
        TransactionType type   = rawAmount.compareTo(BigDecimal.ZERO) < 0
            ? TransactionType.EXPENSE
            : TransactionType.INCOME;
        BigDecimal      amount = rawAmount.abs();

        // Auto-categorise based on payee name keywords
        Category category = autoCategory(payee, categories);

        Transaction t = new Transaction();
        t.setUser(user);
        t.setAccount(account);
        t.setCategory(category);
        t.setType(type);
        t.setAmount(amount);
        t.setPayee(payee != null ? payee.strip() : null);
        t.setDate(date);
        t.setImportHash(rowHash);

        return t;
    }

    /**
     * Try multiple column name variations to get a field value.
     * Different banks use different column names for the same data.
     */
    private String getField(CSVRecord record, String... names) {
        for (String name : names) {
            try {
                String value = record.get(name);
                if (value != null && !value.isBlank()) return value;
            } catch (IllegalArgumentException ignored) {
                // Column doesn't exist in this CSV — try next name
            }
        }
        return null;
    }

    /**
     * Try multiple date formats until one works.
     */
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr.strip(), fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /**
     * Very simple keyword matching to auto-assign categories.
     * In a future version you'd use ML or a rule engine.
     */
    private Category autoCategory(String payee, List<Category> categories) {
        if (payee == null || payee.isBlank()) return null;

        String lower = payee.toLowerCase();

        Map<String, List<String>> keywords = Map.of(
            "Food & Dining",  List.of("restaurant", "cafe", "coffee", "pizza", "burger",
                                      "grocery", "supermarket", "whole foods", "trader joe"),
            "Transport",      List.of("uber", "lyft", "taxi", "fuel", "petrol", "gas station",
                                      "parking", "transit", "bus", "train", "subway"),
            "Shopping",       List.of("amazon", "walmart", "target", "ebay", "shopify",
                                      "clothing", "apparel"),
            "Entertainment",  List.of("netflix", "spotify", "hulu", "disney", "cinema",
                                      "movie", "theatre", "concert", "steam"),
            "Health",         List.of("pharmacy", "doctor", "hospital", "medical", "dental",
                                      "clinic", "cvs", "walgreens"),
            "Utilities",      List.of("electric", "water", "gas", "internet", "phone",
                                      "broadband", "verizon", "at&t"),
            "Housing",        List.of("rent", "mortgage", "landlord", "property")
        );

        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    String categoryName = entry.getKey();
                    return categories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                        .findFirst()
                        .orElse(null);
                }
            }
        }

        return null;
    }

    /**
     * Compute a SHA-256 hash of a string.
     * Used to detect duplicate imports.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}