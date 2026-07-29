package com.aiprototype.engine.controller;

import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.RequirementRequest;
import com.aiprototype.engine.mapper.AnalysisMapper;
import com.aiprototype.engine.service.RequirementAnalyzerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {

    private final RequirementAnalyzerService analyzerService;

    public RequirementController(RequirementAnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody RequirementRequest request) {
        var analysis = analyzerService.analyze(request);
        return ResponseEntity.ok(AnalysisMapper.toResponse(analysis));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Copilot Prototype — requirement-engine is running");
    }
}
