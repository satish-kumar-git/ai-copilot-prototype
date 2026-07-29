package com.aiprototype.engine.service;

import com.aiprototype.core.domain.RequirementAnalysis;
import com.aiprototype.core.dto.RequirementRequest;
import com.aiprototype.core.enums.ScenarioType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RequirementAnalyzerService {

    private static final List<String> VAGUE_TERMS = List.of(
            "scalable", "performant", "fast", "secure", "enterprise",
            "real-time", "reliable", "robust", "flexible", "modern",
            "user-friendly", "intuitive", "seamless", "efficient"
    );

    private final ScenarioClassifierService classifierService;
    private final TaskDecomposerService taskDecomposerService;

    public RequirementAnalyzerService(ScenarioClassifierService classifierService,
                                      TaskDecomposerService taskDecomposerService) {
        this.classifierService = classifierService;
        this.taskDecomposerService = taskDecomposerService;
    }

    public RequirementAnalysis analyze(RequirementRequest request) {
        var classification = classifierService.classify(request.requirement());
        var tasks = taskDecomposerService.decompose(request.requirement(), classification.type());
        var ambiguities = detectAmbiguities(request.requirement());

        return RequirementAnalysis.builder()
                .analysisId(UUID.randomUUID().toString())
                .originalRequirement(request.requirement())
                .scenarioType(classification.type())
                .scenarioRationale(classification.rationale())
                .ambiguities(ambiguities)
                .tasks(tasks)
                .risks(risksFor(classification.type()))
                .assumptions(assumptionsFor(classification.type()))
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    private List<String> detectAmbiguities(String requirement) {
        String lower = requirement.toLowerCase();
        return VAGUE_TERMS.stream()
                .filter(lower::contains)
                .map(term -> "Undefined qualifier '%s' — specify measurable criteria (e.g. p99 latency < 200ms, 10k RPS)".formatted(term))
                .toList();
    }

    private List<String> risksFor(ScenarioType type) {
        return switch (type) {
            case GREENFIELD -> List.of(
                    "Initial architecture decisions are difficult to reverse without clear upfront design",
                    "Scope creep risk is elevated in greenfield projects without strict AC enforcement",
                    "Unknown third-party integration complexity may increase timeline"
            );
            case BROWNFIELD -> List.of(
                    "Undetected dependencies on existing code may break upon modification",
                    "Production data migration risk if schema changes are involved",
                    "Insufficient legacy test coverage increases regression risk"
            );
            case AMBIGUOUS -> List.of(
                    "Starting implementation without clarity will likely result in costly rework",
                    "Misaligned expectations between stakeholders and engineers risk delivery failure",
                    "Inability to estimate effort accurately without a defined scope"
            );
        };
    }

    private List<String> assumptionsFor(ScenarioType type) {
        return switch (type) {
            case GREENFIELD -> List.of(
                    "A new code repository will be created from scratch",
                    "No legacy constraints apply to this implementation",
                    "Team has authority to choose the technology stack"
            );
            case BROWNFIELD -> List.of(
                    "Existing system has at least basic test coverage",
                    "Access to the current codebase and documentation is available",
                    "Backward compatibility is required unless explicitly stated otherwise"
            );
            case AMBIGUOUS -> List.of(
                    "Clarification from the requester is expected before any implementation begins"
            );
        };
    }
}
