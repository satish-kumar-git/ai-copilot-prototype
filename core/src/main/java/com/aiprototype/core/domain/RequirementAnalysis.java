package com.aiprototype.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementAnalysis {

    private String analysisId;
    private String originalRequirement;
    private ScenarioType scenarioType;
    private String scenarioRationale;

    @Builder.Default
    private List<String> identifiedAmbiguities = List.of();

    @Builder.Default
    private List<EngineeringTask> taskBreakdown = List.of();

    @Builder.Default
    private List<String> risks = List.of();

    @Builder.Default
    private List<String> assumptions = List.of();

    private LocalDateTime analyzedAt;
}
