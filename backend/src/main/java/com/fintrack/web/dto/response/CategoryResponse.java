package com.fintrack.web.dto.response;

import com.fintrack.domain.model.Category;
import java.util.UUID;

public record CategoryResponse(
    UUID    id,
    String  name,
    String  icon,
    String  color,
    boolean isSystem
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
            c.getId(), c.getName(), c.getIcon(), c.getColor(), c.isSystem()
        );
    }
}