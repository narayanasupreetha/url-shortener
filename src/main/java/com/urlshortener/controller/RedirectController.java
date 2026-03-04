package com.urlshortener.controller;

import com.urlshortener.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Redirect", description = "Resolve a short code and redirect to the original URL")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RedirectController {

    private final RedirectService redirectService;

    @Operation(summary = "Redirect to the original URL for a given short code")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL",
                    headers = @Header(name = "Location", description = "The original URL",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "404", description = "Short code not found", content = @Content),
            @ApiResponse(responseCode = "410", description = "Short URL is expired or inactive", content = @Content)
    })
    @SecurityRequirements
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(
            @Parameter(description = "The short code to resolve", example = "aB3xYz")
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = redirectService.resolveAndTrack(shortCode, request);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}