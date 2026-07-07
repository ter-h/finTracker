package com.fintrack.web.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Generic wrapper: PageResponse<TransactionResponse>, PageResponse<AccountResponse> etc.
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    // Convert a Spring Page<Entity> into a PageResponse<DTO>
    public static <T, E> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}