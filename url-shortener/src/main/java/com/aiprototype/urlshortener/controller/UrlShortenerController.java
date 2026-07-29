package com.aiprototype.urlshortener.controller;

import com.aiprototype.urlshortener.dto.ShortenRequest;
import com.aiprototype.urlshortener.dto.ShortenResponse;
import com.aiprototype.urlshortener.dto.UrlStatsResponse;
import com.aiprototype.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "Create, redirect, analyse, and manage short URLs")
public class UrlShortenerController {

    private final UrlShortenerService service;

    // ── CREATE ────────────────────────────────────────────────────────

    @PostMapping("/api/v1/urls")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Shorten a URL",
        description = "Creates a 6-character Base62 short code (or uses the supplied customCode). Returns the short URL and metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Short URL created"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "409", description = "Custom code already in use"),
        @ApiResponse(responseCode = "415", description = "Content-Type must be application/json"),
    })
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
        return service.shorten(request);
    }

    // ── REDIRECT ──────────────────────────────────────────────────────

    @GetMapping("/{shortCode:[A-Za-z0-9_-]{3,20}}")
    @Operation(summary = "Redirect to original URL",
        description = "Resolves the short code and returns HTTP 302 to the original URL. Records the click for analytics.")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "404", description = "Short code not found or expired"),
    })
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = service.resolveAndTrack(
            shortCode,
            request.getHeader("User-Agent"),
            request.getRemoteAddr(),
            request.getHeader("Referer")
        );
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

    // ── READ ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Get short URL details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL details returned"),
        @ApiResponse(responseCode = "404", description = "Short code not found"),
    })
    public ShortenResponse getDetails(@PathVariable String shortCode) {
        return service.getDetails(shortCode);
    }

    @GetMapping("/api/v1/urls/{shortCode}/stats")
    @Operation(summary = "Get analytics for a short URL",
        description = "Returns click count, last-clicked timestamp, and daily breakdown for the last 7 days.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "404", description = "Short code not found"),
    })
    public UrlStatsResponse getStats(@PathVariable String shortCode) {
        return service.getStats(shortCode);
    }

    // ── DELETE ────────────────────────────────────────────────────────

    @DeleteMapping("/api/v1/urls/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a short URL",
        description = "Soft-deletes the short URL; existing click history is preserved.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deactivated"),
        @ApiResponse(responseCode = "404", description = "Short code not found"),
    })
    public void delete(@PathVariable String shortCode) {
        service.delete(shortCode);
    }

    // ── HEALTH ────────────────────────────────────────────────────────

    @GetMapping("/api/v1/urls/health")
    @Operation(summary = "Health check")
    public String health() {
        return "URL Shortener is running";
    }
}
