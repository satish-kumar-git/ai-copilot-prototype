package com.aiprototype.engine.mapper;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.RequirementAnalysis;
import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.TaskDto;

public final class AnalysisMapper {

    private AnalysisMapper() {}

    public static AnalysisResponse toResponse(RequirementAnalysis analysis) {
        return new AnalysisResponse(
                analysis.getAnalysisId(),
                analysis.getOriginalRequirement(),
                analysis.getScenarioType(),
                analysis.getScenarioRationale(),
                analysis.getAmbiguities(),
                analysis.getTasks().stream().map(AnalysisMapper::toTaskDto).toList(),
                analysis.getRisks(),
                analysis.getAssumptions(),
                analysis.getAnalyzedAt()
        );
    }

    private static TaskDto toTaskDto(EngineeringTask task) {
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getPriority(),
                task.getDependencies(),
                task.getAiPromptHint()
        );
    }
}
