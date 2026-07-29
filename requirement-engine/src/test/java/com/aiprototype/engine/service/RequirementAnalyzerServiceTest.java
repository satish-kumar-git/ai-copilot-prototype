package com.aiprototype.engine.service;

import com.aiprototype.core.domain.ClassificationResult;
import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.Priority;
import com.aiprototype.core.domain.ScenarioType;
import com.aiprototype.core.domain.TaskCategory;
import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.RequirementRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirementAnalyzerServiceTest {

    @Mock
    private ScenarioClassifierService classifierService;

    @Mock
    private TaskDecomposerService decomposerService;

    @InjectMocks
    private RequirementAnalyzerService analyzerService;

    private RequirementRequest buildRequest(String requirement) {
        return new RequirementRequest(requirement, null);
    }

    private EngineeringTask sampleTask(String id) {
        return EngineeringTask.builder()
                .id(id)
                .title("Sample Task " + id)
                .description("Description for " + id)
                .category(TaskCategory.CONFIGURATION)
                .priority(Priority.HIGH)
                .dependencies(List.of())
                .aiPromptHint("Hint for " + id)
                .build();
    }

    static Stream<ScenarioType> allScenarioTypes() {
        return Stream.of(ScenarioType.values());
    }

    @Test
    void analyze_greenfieldRequirement_returnsAnalysisWithCorrectType() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD,
                        "Requirement uses construction language"));
        when(decomposerService.decompose(any(), any()))
                .thenReturn(List.of(sampleTask("T1"), sampleTask("T2")));

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a new REST API for user management"));

        assertThat(response.getAnalysisId()).isNotNull();
        assertThat(response.getScenarioType()).isEqualTo("GREENFIELD");
        assertThat(response.getTasks()).hasSize(2);
    }

    @Test
    void analyze_scalableKeyword_includesScalableAmbiguity() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a scalable data processing pipeline"));

        assertThat(response.getAmbiguities())
                .anyMatch(a -> a.contains("scalable") && a.contains("undefined"));
    }

    @Test
    void analyze_authKeywordAbsent_includesAuthAmbiguity() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a data processing pipeline"));

        assertThat(response.getAmbiguities())
                .anyMatch(a -> a.contains("Authentication and authorisation scope is not specified"));
    }

    @Test
    void analyze_fastKeyword_includesPerformanceAmbiguity() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a fast data ingestion pipeline"));

        assertThat(response.getAmbiguities())
                .anyMatch(a -> a.contains("p95 latency"));
    }

    @Test
    void analyze_greenfieldType_includesOverengineeringRisk() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a new notification service"));

        assertThat(response.getRisks())
                .anyMatch(r -> r.contains("DESIGN: Over-engineering"));
    }

    @Test
    void analyze_brownfieldType_includesRegressionRisk() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.BROWNFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Fix the existing payment service"));

        assertThat(response.getRisks())
                .anyMatch(r -> r.contains("REGRESSION"));
    }

    @Test
    void analyze_ambiguousType_includesPrematureCodeRisk() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.AMBIGUOUS, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("The system should handle requests properly"));

        assertThat(response.getRisks())
                .anyMatch(r -> r.contains("PREMATURE_CODE"));
    }

    @ParameterizedTest
    @MethodSource("allScenarioTypes")
    void analyze_allTypes_alwaysIncludeAiHallucinationRisk(ScenarioType type) {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(type, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a new notification service"));

        assertThat(response.getRisks())
                .anyMatch(r -> r.contains("AI_HALLUCINATION"));
    }

    @Test
    void analyze_greenfieldAssumptions_containsJava21Stack() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a new notification service"));

        assertThat(response.getAssumptions())
                .anyMatch(a -> a.contains("Java 21"));
    }

    @Test
    void analyze_analyzedAt_isNotNull() {
        when(classifierService.classify(any()))
                .thenReturn(new ClassificationResult(ScenarioType.GREENFIELD, "rationale"));
        when(decomposerService.decompose(any(), any())).thenReturn(List.of());

        AnalysisResponse response = analyzerService.analyze(
                buildRequest("Build a new notification service"));

        assertThat(response.getAnalyzedAt()).isNotNull().isNotBlank();
    }
}
