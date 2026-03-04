package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT login response")
public record LoginResponse(
        @Schema(description = "JWT access token") String accessToken,
        @Schema(description = "Token lifetime in seconds", example = "3600") long expiresIn,
        @Schema(description = "Authenticated user's email", example = "jane@example.com") String email,
        @Schema(description = "Assigned role", example = "ROLE_USER") String role
) {
}