package com.fintrack.service;

import com.fintrack.repository.CategoryRepository;
import com.fintrack.web.dto.response.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAvailable(UUID userId) {
        return categoryRepository
            .findAvailableForUser(userId)
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }
}