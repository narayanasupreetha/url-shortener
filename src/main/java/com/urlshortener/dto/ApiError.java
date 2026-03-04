package com.urlshortener.dto;

import java.time.LocalDateTime;

public record ApiError(
        String code,
        String message,
        String details,
        LocalDateTime timestamp,
        String correlationId
) {
}
