package com.aiprototype.urlshortener.integration;

import com.aiprototype.urlshortener.dto.ShortenRequest;
import com.aiprototype.urlshortener.dto.ShortenResponse;
import com.aiprototype.urlshortener.dto.UrlStatsResponse;
import com.aiprototype.urlshortener.repository.ClickEventRepository;
import com.aiprototype.urlshortener.repository.ShortUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired ClickEventRepository clickEventRepository;

    @AfterEach
    void tearDown() {
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
    }

    // ── full shorten → redirect → stats flow ─────────────────────────

    @Test
    void fullFlow_shortenThenRedirect_clickCountIncremented() throws Exception {
        // 1. Create short URL
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request("https://www.wikipedia.org"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andReturn();

        ShortenResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), ShortenResponse.class);
        String code = created.getShortCode();

        // 2. Redirect (MockMvc does NOT follow the 302)
        mockMvc.perform(get("/" + code))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://www.wikipedia.org"));

        // 3. Stats show 1 click
        MvcResult statsResult = mockMvc.perform(get("/api/v1/urls/" + code + "/stats"))
            .andExpect(status().isOk())
            .andReturn();

        UrlStatsResponse stats = objectMapper.readValue(
            statsResult.getResponse().getContentAsString(), UrlStatsResponse.class);

        assertThat(stats.getClickCount()).isEqualTo(1L);
        assertThat(stats.getShortCode()).isEqualTo(code);
        assertThat(stats.isActive()).isTrue();
        assertThat(stats.isExpired()).isFalse();
    }

    // ── custom code ───────────────────────────────────────────────────

    @Test
    void shorten_withCustomCode_usesCustomCode() throws Exception {
        ShortenRequest req = request("https://www.openai.com");
        req.setCustomCode("openai");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").value("openai"));
    }

    @Test
    void shorten_duplicateCustomCode_returns409() throws Exception {
        ShortenRequest req = request("https://www.example.com");
        req.setCustomCode("dup-code");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        // Second creation with same custom code → 409
        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void fullFlow_shortenThenDelete_redirectReturns404() throws Exception {
        // 1. Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request("https://www.github.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        ShortenResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), ShortenResponse.class);
        String code = created.getShortCode();

        // 2. Delete
        mockMvc.perform(delete("/api/v1/urls/" + code))
            .andExpect(status().isNoContent());

        // 3. Redirect after delete → 404
        mockMvc.perform(get("/" + code))
            .andExpect(status().isNotFound());
    }

    // ── validation ────────────────────────────────────────────────────

    @Test
    void shorten_invalidUrl_returns400() throws Exception {
        ShortenRequest bad = new ShortenRequest();
        bad.setOriginalUrl("not-a-url");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // ── health ────────────────────────────────────────────────────────

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/urls/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("URL Shortener is running"));
    }

    // ── helper ────────────────────────────────────────────────────────

    private ShortenRequest request(String url) {
        ShortenRequest r = new ShortenRequest();
        r.setOriginalUrl(url);
        return r;
    }
}
