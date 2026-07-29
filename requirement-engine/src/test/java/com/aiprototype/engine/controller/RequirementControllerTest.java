package com.aiprototype.engine.controller;

import com.aiprototype.core.dto.RequirementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/requirements/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("AI Copilot Prototype — requirement-engine is running"));
    }

    @Test
    void analyzeReturnsGreenfieldForNewFeatureRequirement() throws Exception {
        var request = new RequirementRequest(
                "Build a new REST API for user authentication and authorization from scratch",
                null
        );

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId", notNullValue()))
                .andExpect(jsonPath("$.scenarioType", is("GREENFIELD")))
                .andExpect(jsonPath("$.tasks", hasSize(8)))
                .andExpect(jsonPath("$.tasks[0].id", is("T-01")))
                .andExpect(jsonPath("$.tasks[0].aiPromptHint", notNullValue()))
                .andExpect(jsonPath("$.risks", hasSize(3)))
                .andExpect(jsonPath("$.assumptions", hasSize(3)));
    }

    @Test
    void analyzeReturnsBrownfieldForBugFixRequirement() throws Exception {
        var request = new RequirementRequest(
                "Fix the critical bug in the existing payment service and refactor the legacy transaction logic",
                null
        );

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioType", is("BROWNFIELD")))
                .andExpect(jsonPath("$.tasks", hasSize(8)));
    }

    @Test
    void analyzeReturnsAmbiguousForVagueRequirement() throws Exception {
        var request = new RequirementRequest(
                "The system should handle all user requests in a proper manner",
                null
        );

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioType", is("AMBIGUOUS")))
                .andExpect(jsonPath("$.tasks", hasSize(3)));
    }

    @Test
    void analyzeDetectsAmbiguousTermsInRequirement() throws Exception {
        var request = new RequirementRequest(
                "Build a new scalable and secure REST API for payments",
                null
        );

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities").isArray())
                .andExpect(jsonPath("$.ambiguities[0]").value(org.hamcrest.Matchers.containsString("scalable")));
    }

    @Test
    void analyzeReturnsBadRequestForBlankRequirement() throws Exception {
        var body = "{\"requirement\": \"\"}";

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void analyzeReturnsBadRequestForTooShortRequirement() throws Exception {
        var body = "{\"requirement\": \"too short\"}";

        mockMvc.perform(post("/api/v1/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
