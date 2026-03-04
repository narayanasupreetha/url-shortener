package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request body for creating a new short URL")
public record CreateUrlRequest(

        @Schema(description = "The original long URL to shorten", example = "https://www.example.com/very/long/path")
        @NotBlank(message = "URL is required")
        @Size(max = 2048)
        String originalUrl,

        @Schema(description = "Optional expiry date/time for the short URL (must be in the future)",
                example = "2026-12-31T23:59:59")
        @Future(message = "Expiry must be in the future")
        LocalDateTime expiryDate
) {
}
