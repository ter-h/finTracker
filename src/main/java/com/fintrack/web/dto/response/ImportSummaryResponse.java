package com.fintrack.web.dto.response;

public record ImportSummaryResponse(
    int totalRows,
    int imported,
    int skipped,       // duplicates
    int failed,        // rows that couldn't be parsed
    String message
) {}