package com.urlshortener.dto;

import com.urlshortener.entity.ShortUrl;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Short URL creation response")
public record ShortUrlResponse(
        @Schema(description = "Database ID", example = "42") Long id,
        @Schema(description = "Full short URL", example = "https://short.ly/aB3xYz") String shortUrl,
        @Schema(description = "Short code segment", example = "aB3xYz") String shortCode,
        @Schema(description = "Original long URL", example = "https://www.example.com/very/long/path") String originalUrl,
        @Schema(description = "Creation timestamp") LocalDateTime createdAt,
        @Schema(description = "Expiry timestamp (null = never expires)") LocalDateTime expiryDate,
        @Schema(description = "Whether the short URL is currently active", example = "true") boolean active
) {
    public static ShortUrlResponse fromEntity(ShortUrl url, String baseUrl) {
        return new ShortUrlResponse(
                url.getId(),
                baseUrl + "/" + url.getShortCode(),
                url.getShortCode(),
                url.getOriginalUrl(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                url.isActive()
        );
    }
}

