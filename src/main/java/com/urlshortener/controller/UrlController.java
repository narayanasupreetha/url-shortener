package com.urlshortener.controller;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.PagedResponse;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "URLs", description = "Create, list, and deactivate short URLs")
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @Operation(summary = "Create a new short URL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created",
                    content = @Content(schema = @Schema(implementation = ShortUrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ShortUrlResponse> create(
            @Valid @RequestBody CreateUrlRequest request,
            Authentication authentication
    ) throws BadRequestException {

        String email = authentication.getName();

        ShortUrlResponse response =
                urlService.createShortUrl(request, email);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "List all short URLs for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of URLs",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<PagedResponse<ShortUrlResponse>> listUserUrls(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {

        if (size < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Size must be greater than 0"
            );
        }

        PagedResponse<ShortUrlResponse> response =
                urlService.getUserUrls(userDetails.getUsername(), page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate a short URL by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "URL deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden – URL belongs to another user", content = @Content),
            @ApiResponse(responseCode = "404", description = "URL not found", content = @Content)
    })
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUrl(
            @Parameter(description = "Short URL database ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        urlService.deactivateUrl(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }
}
