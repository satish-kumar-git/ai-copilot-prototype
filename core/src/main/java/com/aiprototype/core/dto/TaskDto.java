package com.aiprototype.core.dto;

import com.aiprototype.core.enums.Priority;
import com.aiprototype.core.enums.TaskCategory;

import java.util.List;

public record TaskDto(
        String id,
        String title,
        String description,
        TaskCategory category,
        Priority priority,
        List<String> dependencies,
        String aiPromptHint
) {}
