package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "jane@example.com")
        String email,
        @Schema(description = "Account password", example = "Secret42")
        String password
) {
}