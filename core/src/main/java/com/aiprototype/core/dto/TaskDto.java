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
@Schema(description = "A single engineering task derived from the requirement analysis")
public class TaskDto {

    @Schema(description = "Unique task identifier", example = "T-01")
    private String id;

    @Schema(description = "Short task title", example = "Define system requirements and acceptance criteria")
    private String title;

    @Schema(description = "Detailed description of what this task involves and its expected output")
    private String description;

    @Schema(description = "Engineering category this task belongs to", example = "DOCUMENTATION")
    private String category;

    @Schema(description = "Execution priority relative to other tasks", example = "HIGH")
    private String priority;

    @Schema(
            description = "IDs of tasks that must complete before this one can start",
            example = "[\"T-01\", \"T-02\"]"
    )
    private List<String> dependencies;

    @Schema(description = "Self-contained AI-ready prompt that an engineer can paste directly into an LLM to execute this task")
    private String aiPromptHint;
}
