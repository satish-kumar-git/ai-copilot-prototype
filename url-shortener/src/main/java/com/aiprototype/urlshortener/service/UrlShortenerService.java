package com.aiprototype.urlshortener.service;

import com.aiprototype.urlshortener.domain.ClickEvent;
import com.aiprototype.urlshortener.domain.ShortUrl;
import com.aiprototype.urlshortener.dto.ShortenRequest;
import com.aiprototype.urlshortener.dto.ShortenResponse;
import com.aiprototype.urlshortener.dto.UrlStatsResponse;
import com.aiprototype.urlshortener.exception.UrlAlreadyExistsException;
import com.aiprototype.urlshortener.exception.UrlNotFoundException;
import com.aiprototype.urlshortener.repository.ClickEventRepository;
import com.aiprototype.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final Base62Encoder encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    // ── CREATE ────────────────────────────────────────────────────────

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        log.info("Shortening URL: {}", request.getOriginalUrl());
        String code = resolveCode(request);

        ShortUrl shortUrl = ShortUrl.builder()
            .shortCode(code)
            .originalUrl(request.getOriginalUrl())
            .expiresAt(request.getExpiresAt())
            .build();

        shortUrl = shortUrlRepository.save(shortUrl);
        log.info("Created short URL [{}/{}] → {}", baseUrl, code, request.getOriginalUrl());
        return toShortenResponse(shortUrl);
    }

    // ── REDIRECT + TRACK ──────────────────────────────────────────────

    @Transactional
    public String resolveAndTrack(String shortCode, String userAgent, String ipAddress, String referer) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCodeAndActiveTrue(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (shortUrl.isExpired()) {
            log.warn("Redirect attempted for expired short code: {}", shortCode);
            throw new UrlNotFoundException(shortCode);
        }

        // Atomic increment — avoids lost-update under concurrent requests
        shortUrlRepository.incrementClickCount(shortUrl.getId());

        clickEventRepository.save(ClickEvent.builder()
            .shortUrl(shortUrl)
            .userAgent(truncate(userAgent, 512))
            .ipAddress(truncate(ipAddress, 45))
            .referer(truncate(referer, 2048))
            .build());

        log.info("Redirect {} → {}", shortCode, shortUrl.getOriginalUrl());
        return shortUrl.getOriginalUrl();
    }

    // ── READ ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ShortenResponse getDetails(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return toShortenResponse(shortUrl);
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));

        List<ClickEvent> events = clickEventRepository.findByShortUrlOrderByClickedAtDesc(shortUrl);

        String lastClickedAt = events.isEmpty() ? null
            : events.get(0).getClickedAt().toString();

        List<UrlStatsResponse.DailyClickCount> dailyStats = buildDailyStats(events);

        return UrlStatsResponse.builder()
            .shortCode(shortUrl.getShortCode())
            .shortUrl(baseUrl + "/" + shortUrl.getShortCode())
            .originalUrl(shortUrl.getOriginalUrl())
            .clickCount(shortUrl.getClickCount())
            .createdAt(shortUrl.getCreatedAt().toString())
            .expiresAt(shortUrl.getExpiresAt() != null ? shortUrl.getExpiresAt().toString() : null)
            .lastClickedAt(lastClickedAt)
            .active(shortUrl.getActive())
            .expired(shortUrl.isExpired())
            .dailyStats(dailyStats)
            .build();
    }

    // ── DELETE ────────────────────────────────────────────────────────

    @Transactional
    public void delete(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));
        shortUrl.setActive(false);
        shortUrlRepository.save(shortUrl);
        log.info("Deactivated short URL: {}", shortCode);
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private String resolveCode(ShortenRequest request) {
        String custom = request.getCustomCode();
        if (custom != null && !custom.isBlank()) {
            if (shortUrlRepository.existsByShortCode(custom)) {
                throw new UrlAlreadyExistsException(custom);
            }
            return custom;
        }
        return generateUniqueCode();
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            // Range [62^5, 62^6) always produces exactly 6-character Base62 codes
            long value = ThreadLocalRandom.current().nextLong(
                Base62Encoder.MIN_6_CHAR, Base62Encoder.MAX_6_CHAR + 1);
            code = encoder.encode(value);
            attempts++;
        } while (shortUrlRepository.existsByShortCode(code) && attempts < 10);

        if (attempts >= 10) {
            throw new IllegalStateException("Unable to generate a unique short code after 10 attempts");
        }
        return code;
    }

    private List<UrlStatsResponse.DailyClickCount> buildDailyStats(List<ClickEvent> events) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        Map<LocalDate, Long> byDay = events.stream()
            .filter(e -> e.getClickedAt().isAfter(cutoff))
            .collect(Collectors.groupingBy(
                e -> e.getClickedAt().toLocalDate(),
                TreeMap::new,
                Collectors.counting()
            ));

        return byDay.entrySet().stream()
            .map(entry -> UrlStatsResponse.DailyClickCount.builder()
                .date(entry.getKey().toString())
                .count(entry.getValue())
                .build())
            .toList();
    }

    private ShortenResponse toShortenResponse(ShortUrl s) {
        return ShortenResponse.builder()
            .shortCode(s.getShortCode())
            .shortUrl(baseUrl + "/" + s.getShortCode())
            .originalUrl(s.getOriginalUrl())
            .createdAt(s.getCreatedAt().toString())
            .expiresAt(s.getExpiresAt() != null ? s.getExpiresAt().toString() : null)
            .clickCount(s.getClickCount())
            .build();
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
