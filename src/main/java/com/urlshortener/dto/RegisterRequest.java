package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user registration")
public record RegisterRequest(

        @Schema(description = "Full name of the user", example = "Jane Doe")
        @NotBlank
        String name,

        @Schema(description = "Email address (used as login)", example = "jane@example.com")
        @Email(message = "Invalid email format")
        @NotBlank
        String email,

        @Schema(description = "Password – minimum 8 chars, must contain a letter and a number",
                example = "Secret42")
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number"
        )
        String password
) {
}
