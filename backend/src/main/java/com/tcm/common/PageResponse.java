package com.tcm.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Standard paginated response shape, per docs/PLAN.md §6. Every future
 * paginated listing endpoint wraps its {@link Page} in this.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
