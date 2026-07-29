package com.aiprototype.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full structured analysis result returned for a given software requirement")
public class AnalysisResponse {

    @Schema(description = "Unique identifier for this analysis run", example = "a3f2c1d4-9b8e-4f2a-b1c3-7d6e5f4a3b2c")
    private String analysisId;

    @Schema(description = "The original requirement text exactly as submitted")
    private String originalRequirement;

    @Schema(description = "Classified scenario type: GREENFIELD, BROWNFIELD, or AMBIGUOUS", example = "GREENFIELD")
    private String scenarioType;

    @Schema(description = "Explanation of why this scenario type was assigned, including matched keyword signals")
    private String scenarioRationale;

    @Schema(description = "List of detected ambiguous qualifiers with suggestions on how to make them measurable")
    private List<String> ambiguities;

    @Schema(description = "Ordered list of engineering tasks decomposed from the requirement, each with an AI prompt hint")
    private List<TaskDto> tasks;

    @Schema(description = "Top engineering risks identified for this type of work")
    private List<String> risks;

    @Schema(description = "Baseline assumptions the analysis was made under")
    private List<String> assumptions;

    @Schema(description = "ISO-8601 timestamp of when this analysis was performed", example = "2026-07-29T14:30:00")
    private String analyzedAt;
}
