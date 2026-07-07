package com.fintrack.service.report;

import com.fintrack.domain.model.Transaction;
import com.fintrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final TransactionRepository transactionRepository;

    public byte[] exportTransactionsCsv(UUID userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions =
            transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                userId, from, to
            );

        try (
            ByteArrayOutputStream baos   = new ByteArrayOutputStream();
            OutputStreamWriter    writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            CSVPrinter            csv    = new CSVPrinter(writer, CSVFormat.DEFAULT
                .builder()
                .setHeader("Date", "Payee", "Category", "Type", "Amount",
                           "Account", "Notes")
                .build())
        ) {
            for (Transaction t : transactions) {
                csv.printRecord(
                    t.getDate(),
                    t.getPayee()    != null ? t.getPayee()              : "",
                    t.getCategory() != null ? t.getCategory().getName() : "",
                    t.getType().name(),
                    t.getAmount().toPlainString(),
                    t.getAccount().getName(),
                    t.getNotes() != null ? t.getNotes() : ""
                );
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV: " + e.getMessage(), e);
        }
    }
}