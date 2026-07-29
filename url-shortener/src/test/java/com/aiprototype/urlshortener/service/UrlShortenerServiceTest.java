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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock ShortUrlRepository shortUrlRepository;
    @Mock ClickEventRepository clickEventRepository;
    @Mock Base62Encoder encoder;
    @InjectMocks UrlShortenerService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8091");
    }

    // ── shorten ───────────────────────────────────────────────────────

    @Test
    void shorten_validRequest_returnsShortUrl() {
        when(encoder.encode(anyLong())).thenReturn("aB3xY9");
        when(shortUrlRepository.existsByShortCode("aB3xY9")).thenReturn(false);

        ShortUrl saved = shortUrl("aB3xY9", "https://example.com");
        when(shortUrlRepository.save(any())).thenReturn(saved);

        ShortenResponse response = service.shorten(request("https://example.com", null));

        assertThat(response.getShortCode()).isEqualTo("aB3xY9");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8091/aB3xY9");
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void shorten_customCode_usesProvidedCode() {
        when(shortUrlRepository.existsByShortCode("my-link")).thenReturn(false);

        ShortUrl saved = shortUrl("my-link", "https://example.com");
        when(shortUrlRepository.save(any())).thenReturn(saved);

        ShortenResponse response = service.shorten(request("https://example.com", "my-link"));

        assertThat(response.getShortCode()).isEqualTo("my-link");
        verify(encoder, never()).encode(anyLong()); // no generation needed
    }

    @Test
    void shorten_duplicateCustomCode_throwsConflict() {
        when(shortUrlRepository.existsByShortCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.shorten(request("https://example.com", "taken")))
            .isInstanceOf(UrlAlreadyExistsException.class)
            .hasMessageContaining("taken");
    }

    // ── resolveAndTrack ───────────────────────────────────────────────

    @Test
    void resolveAndTrack_existingCode_returnsOriginalUrl() {
        ShortUrl url = shortUrl("abc123", "https://target.com");
        when(shortUrlRepository.findByShortCodeAndActiveTrue("abc123")).thenReturn(Optional.of(url));

        String result = service.resolveAndTrack("abc123", "Mozilla/5.0", "127.0.0.1", null);

        assertThat(result).isEqualTo("https://target.com");
        verify(shortUrlRepository).incrementClickCount(url.getId());
        verify(clickEventRepository).save(any(ClickEvent.class));
    }

    @Test
    void resolveAndTrack_unknownCode_throwsNotFound() {
        when(shortUrlRepository.findByShortCodeAndActiveTrue("xyz")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndTrack("xyz", null, null, null))
            .isInstanceOf(UrlNotFoundException.class)
            .hasMessageContaining("xyz");
    }

    @Test
    void resolveAndTrack_expiredUrl_throwsNotFound() {
        ShortUrl expired = ShortUrl.builder()
            .id(1L).shortCode("old1").originalUrl("https://gone.com")
            .createdAt(LocalDateTime.now().minusDays(10))
            .expiresAt(LocalDateTime.now().minusDays(1)) // already expired
            .clickCount(0L).active(true).build();

        when(shortUrlRepository.findByShortCodeAndActiveTrue("old1")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveAndTrack("old1", null, null, null))
            .isInstanceOf(UrlNotFoundException.class);
    }

    // ── getStats ──────────────────────────────────────────────────────

    @Test
    void getStats_existingCode_returnsStatsSummary() {
        ShortUrl url = shortUrl("stat1", "https://stats.com");
        url.setClickCount(5L);
        when(shortUrlRepository.findByShortCode("stat1")).thenReturn(Optional.of(url));
        when(clickEventRepository.findByShortUrlOrderByClickedAtDesc(url)).thenReturn(List.of());

        UrlStatsResponse stats = service.getStats("stat1");

        assertThat(stats.getShortCode()).isEqualTo("stat1");
        assertThat(stats.getClickCount()).isEqualTo(5L);
        assertThat(stats.isActive()).isTrue();
        assertThat(stats.isExpired()).isFalse();
        assertThat(stats.getDailyStats()).isEmpty();
    }

    @Test
    void getStats_unknownCode_throwsNotFound() {
        when(shortUrlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats("nope"))
            .isInstanceOf(UrlNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_existingCode_deactivatesUrl() {
        ShortUrl url = shortUrl("del1", "https://delete.com");
        when(shortUrlRepository.findByShortCode("del1")).thenReturn(Optional.of(url));
        when(shortUrlRepository.save(any())).thenReturn(url);

        service.delete("del1");

        assertThat(url.getActive()).isFalse();
        verify(shortUrlRepository).save(url);
    }

    @Test
    void delete_unknownCode_throwsNotFound() {
        when(shortUrlRepository.findByShortCode("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("gone"))
            .isInstanceOf(UrlNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private ShortenRequest request(String url, String customCode) {
        ShortenRequest r = new ShortenRequest();
        r.setOriginalUrl(url);
        r.setCustomCode(customCode);
        return r;
    }

    private ShortUrl shortUrl(String code, String originalUrl) {
        return ShortUrl.builder()
            .id(1L)
            .shortCode(code)
            .originalUrl(originalUrl)
            .createdAt(LocalDateTime.now())
            .clickCount(0L)
            .active(true)
            .build();
    }
}
