package com.urlshortener.dto;

import java.util.List;

public record PagedResponse<T>(
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        List<T> content
) {
}