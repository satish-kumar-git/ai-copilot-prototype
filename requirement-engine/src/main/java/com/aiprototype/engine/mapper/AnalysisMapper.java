package com.aiprototype.engine.mapper;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.RequirementAnalysis;
import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.TaskDto;

public final class AnalysisMapper {

    private AnalysisMapper() {}

    public static AnalysisResponse toResponse(RequirementAnalysis analysis) {
        return AnalysisResponse.builder()
                .analysisId(analysis.getAnalysisId())
                .originalRequirement(analysis.getOriginalRequirement())
                .scenarioType(analysis.getScenarioType().name())
                .scenarioRationale(analysis.getScenarioRationale())
                .ambiguities(analysis.getIdentifiedAmbiguities())
                .tasks(analysis.getTaskBreakdown().stream().map(AnalysisMapper::toTaskDto).toList())
                .risks(analysis.getRisks())
                .assumptions(analysis.getAssumptions())
                .analyzedAt(analysis.getAnalyzedAt().toString())
                .build();
    }

    private static TaskDto toTaskDto(EngineeringTask task) {
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
