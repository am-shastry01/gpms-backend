package com.gpms.backend.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A stable JSON shape for paged results.
 *
 * Returning Spring's Page/PageImpl straight from a controller works
 * but its JSON structure is not part of Spring's API contract, and
 * Boot logs a warning about serialising it. The field names here
 * match what PageImpl produced, so existing clients keep working.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }
}
