package com.aiprototype.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Created short URL details")
public class ShortenResponse {

    @Schema(description = "The generated or custom short code", example = "aB3xY9")
    private String shortCode;

    @Schema(description = "The full short URL to share", example = "http://localhost:8091/aB3xY9")
    private String shortUrl;

    @Schema(description = "The original long URL", example = "https://www.example.com/long/path")
    private String originalUrl;

    @Schema(description = "ISO-8601 creation timestamp")
    private String createdAt;

    @Schema(description = "ISO-8601 expiry timestamp, null if no expiry", nullable = true)
    private String expiresAt;

    @Schema(description = "Total number of times this link has been followed")
    private Long clickCount;
}
