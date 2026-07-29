package com.aiprototype.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringTask {

    private String id;
    private String title;
    private String description;
    private TaskCategory category;
    private Priority priority;

    @Builder.Default
    private List<String> dependencies = List.of();

    private String aiPromptHint;
}
