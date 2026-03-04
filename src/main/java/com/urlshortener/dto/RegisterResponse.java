package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration response")
public record RegisterResponse(
        @Schema(description = "Assigned database ID", example = "1") Long id,
        @Schema(description = "Registered email", example = "jane@example.com") String email
) {
}