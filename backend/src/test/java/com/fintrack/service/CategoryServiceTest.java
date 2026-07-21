package com.fintrack.service;

import com.fintrack.domain.model.Category;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.web.dto.response.CategoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void getAvailable_returnsMappedCategoryList() {
        UUID userId = UUID.randomUUID();

        Category system = new Category();
        system.setId(UUID.randomUUID());
        system.setName("Groceries");
        system.setIcon("cart");
        system.setColor("#00ff00");
        system.setSystem(true);

        Category custom = new Category();
        custom.setId(UUID.randomUUID());
        custom.setName("Hobbies");
        custom.setIcon("star");
        custom.setColor("#ff00ff");
        custom.setSystem(false);

        when(categoryRepository.findAvailableForUser(userId))
            .thenReturn(List.of(system, custom));

        List<CategoryResponse> result = categoryService.getAvailable(userId);

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(CategoryResponse::name)
            .containsExactly("Groceries", "Hobbies");
        assertThat(result.get(0).isSystem()).isTrue();
        assertThat(result.get(1).isSystem()).isFalse();
    }

    @Test
    void getAvailable_whenNoneAvailable_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getAvailable(userId);

        assertThat(result).isEmpty();
    }
}
