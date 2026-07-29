package com.aiprototype.core.domain;

import com.aiprototype.core.enums.Priority;
import com.aiprototype.core.enums.TaskCategory;

import java.util.List;

public class EngineeringTask {

    private final String id;
    private final String title;
    private final String description;
    private final TaskCategory category;
    private final Priority priority;
    private final List<String> dependencies;
    private final String aiPromptHint;

    private EngineeringTask(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.category = builder.category;
        this.priority = builder.priority;
        this.dependencies = builder.dependencies;
        this.aiPromptHint = builder.aiPromptHint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskCategory getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public List<String> getDependencies() { return dependencies; }
    public String getAiPromptHint() { return aiPromptHint; }

    public static class Builder {
        private String id;
        private String title;
        private String description;
        private TaskCategory category;
        private Priority priority;
        private List<String> dependencies;
        private String aiPromptHint;

        public Builder id(String id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(TaskCategory category) { this.category = category; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder dependencies(List<String> dependencies) { this.dependencies = dependencies; return this; }
        public Builder aiPromptHint(String aiPromptHint) { this.aiPromptHint = aiPromptHint; return this; }

        public EngineeringTask build() {
            return new EngineeringTask(this);
        }
    }
}
