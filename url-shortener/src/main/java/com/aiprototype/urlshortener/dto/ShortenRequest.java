package com.aiprototype.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Request payload to create a short URL")
public class ShortenRequest {

    @NotBlank(message = "originalUrl is required")
    @Pattern(
        regexp = "^https?://[^\\s]+$",
        message = "originalUrl must be a valid http or https URL"
    )
    @Size(max = 2048, message = "originalUrl must not exceed 2048 characters")
    @Schema(description = "The long URL to shorten", example = "https://www.example.com/some/very/long/path?query=value")
    private String originalUrl;

    @Size(min = 3, max = 20, message = "customCode must be 3–20 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9_-]*$",
        message = "customCode may only contain letters, digits, hyphens, and underscores"
    )
    @Schema(description = "Optional custom short code (3–20 alphanumeric chars)", example = "my-link", nullable = true)
    private String customCode;

    @Future(message = "expiresAt must be a future date-time")
    @Schema(description = "Optional expiry date-time (ISO-8601)", example = "2027-01-01T00:00:00", nullable = true)
    private LocalDateTime expiresAt;
}
