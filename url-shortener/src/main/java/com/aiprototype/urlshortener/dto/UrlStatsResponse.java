package com.aiprototype.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Analytics for a short URL")
public class UrlStatsResponse {

    @Schema(description = "Short code identifier")
    private String shortCode;

    @Schema(description = "Full short URL")
    private String shortUrl;

    @Schema(description = "Original long URL")
    private String originalUrl;

    @Schema(description = "Total click count")
    private Long clickCount;

    @Schema(description = "ISO-8601 creation timestamp")
    private String createdAt;

    @Schema(description = "ISO-8601 expiry timestamp, null if no expiry", nullable = true)
    private String expiresAt;

    @Schema(description = "ISO-8601 timestamp of the most recent click, null if never clicked", nullable = true)
    private String lastClickedAt;

    @Schema(description = "Whether this short URL is active")
    private boolean active;

    @Schema(description = "Whether this short URL has passed its expiry date")
    private boolean expired;

    @Schema(description = "Click counts grouped by day (last 7 days)")
    private List<DailyClickCount> dailyStats;

    @Data
    @Builder
    @Schema(description = "Click count for a single calendar day")
    public static class DailyClickCount {
        @Schema(description = "Calendar date in YYYY-MM-DD format", example = "2026-07-29")
        private String date;

        @Schema(description = "Number of clicks on this day")
        private Long count;
    }
}
