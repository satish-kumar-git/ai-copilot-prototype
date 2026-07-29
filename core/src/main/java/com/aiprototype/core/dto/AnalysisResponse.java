package com.aiprototype.core.dto;

import com.aiprototype.core.enums.ScenarioType;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisResponse(
        String analysisId,
        String originalRequirement,
        ScenarioType scenarioType,
        String scenarioRationale,
        List<String> ambiguities,
        List<TaskDto> tasks,
        List<String> risks,
        List<String> assumptions,
        LocalDateTime analyzedAt
) {}
