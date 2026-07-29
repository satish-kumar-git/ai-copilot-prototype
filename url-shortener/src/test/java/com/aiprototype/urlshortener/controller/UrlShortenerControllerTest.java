package com.aiprototype.urlshortener.controller;

import com.aiprototype.urlshortener.dto.ShortenRequest;
import com.aiprototype.urlshortener.dto.ShortenResponse;
import com.aiprototype.urlshortener.dto.UrlStatsResponse;
import com.aiprototype.urlshortener.exception.GlobalExceptionHandler;
import com.aiprototype.urlshortener.exception.UrlNotFoundException;
import com.aiprototype.urlshortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
@Import(GlobalExceptionHandler.class)
class UrlShortenerControllerTest {

    private static final String API_URLS = "/api/v1/urls";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UrlShortenerService service;

    // ── POST /api/v1/urls ─────────────────────────────────────────────

    @Test
    void shorten_validRequest_returns201() throws Exception {
        ShortenResponse resp = response("aB3xY9", "https://example.com");
        when(service.shorten(any())).thenReturn(resp);

        mockMvc.perform(post(API_URLS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").value("aB3xY9"))
            .andExpect(jsonPath("$.shortUrl").value("http://localhost:8091/aB3xY9"))
            .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    void shorten_blankUrl_returns400ValidationError() throws Exception {
        ShortenRequest bad = new ShortenRequest();
        bad.setOriginalUrl("");

        mockMvc.perform(post(API_URLS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
    }

    @Test
    void shorten_missingContentType_returns415() throws Exception {
        mockMvc.perform(post(API_URLS)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shorten_invalidUrlScheme_returns400() throws Exception {
        ShortenRequest bad = new ShortenRequest();
        bad.setOriginalUrl("ftp://not-http.com");

        mockMvc.perform(post(API_URLS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // ── GET /{shortCode} ──────────────────────────────────────────────

    @Test
    void redirect_existingCode_returns302WithLocation() throws Exception {
        when(service.resolveAndTrack(eq("aB3xY9"), any(), any(), any()))
            .thenReturn("https://example.com/long/path");

        mockMvc.perform(get("/aB3xY9"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com/long/path"));
    }

    @Test
    void redirect_unknownCode_returns404() throws Exception {
        when(service.resolveAndTrack(eq("nocode"), any(), any(), any()))
            .thenThrow(new UrlNotFoundException("nocode"));

        mockMvc.perform(get("/nocode"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ── GET /api/v1/urls/{shortCode}/stats ────────────────────────────

    @Test
    void getStats_existingCode_returns200() throws Exception {
        UrlStatsResponse stats = UrlStatsResponse.builder()
            .shortCode("aB3xY9").shortUrl("http://localhost:8091/aB3xY9")
            .originalUrl("https://example.com").clickCount(42L)
            .createdAt("2026-07-29T10:00:00").active(true).expired(false)
            .dailyStats(List.of()).build();

        when(service.getStats("aB3xY9")).thenReturn(stats);

        mockMvc.perform(get(API_URLS + "/aB3xY9/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clickCount").value(42))
            .andExpect(jsonPath("$.active").value(true));
    }

    // ── DELETE /api/v1/urls/{shortCode} ───────────────────────────────

    @Test
    void delete_existingCode_returns204() throws Exception {
        doNothing().when(service).delete("aB3xY9");

        mockMvc.perform(delete(API_URLS + "/aB3xY9"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_unknownCode_returns404() throws Exception {
        doThrow(new UrlNotFoundException("gone")).when(service).delete("gone");

        mockMvc.perform(delete(API_URLS + "/gone"))
            .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/urls/health ───────────────────────────────────────

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get(API_URLS + "/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("URL Shortener is running"));
    }

    // ── helpers ───────────────────────────────────────────────────────

    private ShortenRequest validRequest() {
        ShortenRequest r = new ShortenRequest();
        r.setOriginalUrl("https://example.com/some/long/path");
        return r;
    }

    private ShortenResponse response(String code, String original) {
        return ShortenResponse.builder()
            .shortCode(code)
            .shortUrl("http://localhost:8091/" + code)
            .originalUrl(original)
            .createdAt("2026-07-29T10:00:00")
            .clickCount(0L)
            .build();
    }
}
