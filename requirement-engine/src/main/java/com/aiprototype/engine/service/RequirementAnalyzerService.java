package com.aiprototype.engine.service;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.ScenarioType;
import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.RequirementRequest;
import com.aiprototype.core.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementAnalyzerService {

    private final ScenarioClassifierService classifierService;
    private final TaskDecomposerService decomposerService;

    public AnalysisResponse analyze(RequirementRequest request) {
        String requirement = request.getRequirement();
        log.info("Analyzing requirement [{} chars]", requirement.length());

        var classification = classifierService.classify(requirement);
        List<String> ambiguities = identifyAmbiguities(requirement);
        var tasks = decomposerService.decompose(requirement, classification.type());
        List<String> risks = buildRisks(classification.type());
        List<String> assumptions = buildAssumptions(classification.type());

        log.info("Analysis complete: type={}, tasks={}, ambiguities={}",
                classification.type(), tasks.size(), ambiguities.size());

        return AnalysisResponse.builder()
                .analysisId(UUID.randomUUID().toString())
                .originalRequirement(requirement)
                .scenarioType(classification.type().name())
                .scenarioRationale(classification.rationale())
                .ambiguities(ambiguities)
                .tasks(tasks.stream().map(this::toDto).toList())
                .risks(risks)
                .assumptions(assumptions)
                .analyzedAt(LocalDateTime.now().toString())
                .build();
    }

    private List<String> identifyAmbiguities(String requirement) {
        String lower = requirement.toLowerCase();
        List<String> ambiguities = new ArrayList<>();

        if (lower.contains("scalable") || lower.contains("scale")) {
            ambiguities.add("'scalable' is undefined — specify: expected requests/second, concurrent users, and data volume at peak");
        }
        if (lower.contains("fast") || lower.contains("slow") || lower.contains("performance")) {
            ambiguities.add("'Fast'/'slow'/'performant' is not measurable — define a p95 latency target (e.g. < 50ms)");
        }
        if (!lower.contains("auth") && !lower.contains("login") && !lower.contains("user")
                && !lower.contains("jwt") && !lower.contains("token")) {
            ambiguities.add("Authentication and authorisation scope is not specified — public API or secured?");
        }
        if (!lower.contains("database") && !lower.contains("db") && !lower.contains("persist")
                && !lower.contains("storage") && !lower.contains("sql")
                && !lower.contains("postgres") && !lower.contains("mysql")) {
            ambiguities.add("Persistence technology and durability requirements are not specified");
        }
        if (lower.contains("improve") || lower.contains("better") || lower.contains("enhance")
                || lower.contains("optimize")) {
            ambiguities.add("'Improve'/'enhance'/'optimize' is vague — define the before-state baseline and a measurable success threshold");
        }
        if (!lower.contains("sla") && !lower.contains("availability") && !lower.contains("uptime")
                && !lower.contains("99.")) {
            ambiguities.add("Availability and SLA requirements are not stated");
        }

        return ambiguities;
    }

    private List<String> buildRisks(ScenarioType type) {
        List<String> risks = new ArrayList<>(switch (type) {
            case GREENFIELD -> List.of(
                    "DESIGN: Over-engineering early — build for current validated requirements, not speculative future scale",
                    "SECURITY: No authentication specified — public API is a production security risk",
                    "DESIGN: H2 in-memory loses all data on restart — acceptable for prototype, requires PostgreSQL for production",
                    "DESIGN: Cache invalidation on mutation — ensure @CacheEvict is applied on all state-mutating operations",
                    "PERFORMANCE: Synchronous analytics recording blocks the critical path — use @Async fire-and-forget"
            );
            case BROWNFIELD -> List.of(
                    "REGRESSION: Existing tests must pass before and after the change — run full suite at both points",
                    "SCHEMA: Flyway migration must be backwards-compatible or a maintenance window must be planned",
                    "CACHE: Stale cache entries — verify whether any cache needs invalidation after this change",
                    "API: Breaking changes to existing endpoints require a versioning strategy (v2 path or deprecation header)"
            );
            case AMBIGUOUS -> List.of(
                    "PREMATURE_CODE: Writing code before validating the hypothesis wastes effort and may solve the wrong problem",
                    "WRONG_BOTTLENECK: Optimising a component that is not actually the bottleneck — measure first",
                    "SCOPE_CREEP: Once a hypothesis is validated, constrain the fix to only that — do not solve adjacent problems"
            );
        });
        risks.add("AI_HALLUCINATION: AI-generated code may reference non-existent API methods or packages — verify each import against library Javadoc");
        risks.add("AI_COVERAGE_GAP: AI-generated tests often miss edge cases — review each test method and add: null inputs, boundary values, exception paths");
        return risks;
    }

    private List<String> buildAssumptions(ScenarioType type) {
        return switch (type) {
            case GREENFIELD -> List.of(
                    "Java 21 + Spring Boot 3.x is the target stack",
                    "H2 in-memory database is acceptable for the prototype phase",
                    "Authentication and authorisation are out of scope for the initial delivery",
                    "All configuration is externalised via environment variables or application.yml",
                    "Local development environment uses Docker Compose; Kubernetes is out of scope"
            );
            case BROWNFIELD -> List.of(
                    "The existing test suite is the regression baseline — no tests may be deleted",
                    "No breaking changes will be introduced to existing API contracts without a versioning strategy",
                    "Flyway is the sole mechanism for schema changes — no manual DDL is permitted",
                    "Feature flags will not be used unless the change carries deployment risk requiring a rollback path"
            );
            case AMBIGUOUS -> List.of(
                    "No code will be written until acceptance criteria are agreed and documented",
                    "Each hypothesis will be validated against real production data or a representative dataset",
                    "Implementation will be constrained to the single validated hypothesis — no adjacent improvements"
            );
        };
    }

    private TaskDto toDto(EngineeringTask task) {
        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory().name())
                .priority(task.getPriority().name())
                .dependencies(task.getDependencies())
                .aiPromptHint(task.getAiPromptHint())
                .build();
    }
}
