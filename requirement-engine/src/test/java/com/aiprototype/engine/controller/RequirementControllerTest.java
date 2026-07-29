package com.aiprototype.engine.controller;

import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.RequirementRequest;
import com.aiprototype.engine.exception.GlobalExceptionHandler;
import com.aiprototype.engine.service.RequirementAnalyzerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequirementController.class)
@Import(GlobalExceptionHandler.class)
class RequirementControllerTest {

    private static final String URL = "/api/v1/requirements/analyze";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequirementAnalyzerService analyzerService;

    private AnalysisResponse sampleResponse() {
        return AnalysisResponse.builder()
                .analysisId("test-id-123")
                .originalRequirement("Build a REST API for user management")
                .scenarioType("GREENFIELD")
                .scenarioRationale("Requirement uses construction language indicating a new system or feature")
                .ambiguities(List.of("Authentication and authorisation scope is not specified"))
                .tasks(List.of())
                .risks(List.of("DESIGN: Over-engineering early — build for current validated requirements"))
                .assumptions(List.of("Java 21 + Spring Boot 3.x is the target stack"))
                .analyzedAt("2026-07-29T12:00:00")
                .build();
    }

    @Test
    void analyze_validRequest_returns200WithAnalysis() throws Exception {
        when(analyzerService.analyze(any())).thenReturn(sampleResponse());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RequirementRequest("Build a new REST API for user management", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId", is("test-id-123")))
                .andExpect(jsonPath("$.scenarioType", is("GREENFIELD")));
    }

    @Test
    void analyze_blankRequirement_returns400ValidationError() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void analyze_nullRequirement_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyze_requirementTooShort_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void analyze_requirementTooLong_returns400() throws Exception {
        String tooLong = "a".repeat(5001);
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyze_withContext_returns200() throws Exception {
        when(analyzerService.analyze(any())).thenReturn(sampleResponse());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequirementRequest(
                                "Build a new REST API for user management",
                                "Existing Spring Boot 3.x service, Java 21, PostgreSQL"))))
                .andExpect(status().isOk());
    }

    @Test
    void analyze_serviceThrowsRuntimeException_returns500() throws Exception {
        when(analyzerService.analyze(any()))
                .thenThrow(new RuntimeException("Unexpected failure"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RequirementRequest("Build a new REST API for user management", null))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("INTERNAL_ERROR")));
    }

    @Test
    void health_returns200WithMessage() throws Exception {
        mockMvc.perform(get("/api/v1/requirements/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Requirement Engine is running"));
    }

    @Test
    void analyze_missingContentType_returns415() throws Exception {
        mockMvc.perform(post(URL)
                        .content("{\"requirement\": \"Build a new REST API for user management\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
