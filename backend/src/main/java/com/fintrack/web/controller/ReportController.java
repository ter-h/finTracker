package com.fintrack.web.controller;

import com.fintrack.security.UserPrincipal;
import com.fintrack.service.report.CsvExportService;
import com.fintrack.service.report.PdfExportService;
import com.fintrack.service.report.ReportService;
import com.fintrack.web.dto.response.IncomeExpenseTrendResponse;
import com.fintrack.web.dto.response.SpendingByCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService    reportService;
    private final PdfExportService pdfExportService;
    private final CsvExportService csvExportService;

    // GET /api/v1/reports/spending-by-category?from=2024-06-01&to=2024-06-30
    @GetMapping("/spending-by-category")
    public ResponseEntity<SpendingByCategoryResponse> spendingByCategory(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            reportService.spendingByCategory(principal.getId(), from, to)
        );
    }

    // GET /api/v1/reports/income-vs-expense?months=6
    @GetMapping("/income-vs-expense")
    public ResponseEntity<IncomeExpenseTrendResponse> incomeVsExpense(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(
            reportService.incomeVsExpenseTrend(principal.getId(), months)
        );
    }

    // GET /api/v1/reports/export?format=pdf&from=2024-06-01&to=2024-06-30
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "csv") String format,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        UUID userId = principal.getId();

        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = pdfExportService.exportTransactionsPdf(userId, from, to);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                        .filename("fintrack-report.pdf").build().toString())
                .body(pdf);
        }

        // Default: CSV
        byte[] csv = csvExportService.exportTransactionsCsv(userId, from, to);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("fintrack-transactions.csv").build().toString())
            .body(csv);
    }
}