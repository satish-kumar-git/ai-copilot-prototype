package com.aiprototype.core.domain;

import com.aiprototype.core.enums.ScenarioType;

import java.time.LocalDateTime;
import java.util.List;

public class RequirementAnalysis {

    private final String analysisId;
    private final String originalRequirement;
    private final ScenarioType scenarioType;
    private final String scenarioRationale;
    private final List<String> ambiguities;
    private final List<EngineeringTask> tasks;
    private final List<String> risks;
    private final List<String> assumptions;
    private final LocalDateTime analyzedAt;

    private RequirementAnalysis(Builder builder) {
        this.analysisId = builder.analysisId;
        this.originalRequirement = builder.originalRequirement;
        this.scenarioType = builder.scenarioType;
        this.scenarioRationale = builder.scenarioRationale;
        this.ambiguities = builder.ambiguities;
        this.tasks = builder.tasks;
        this.risks = builder.risks;
        this.assumptions = builder.assumptions;
        this.analyzedAt = builder.analyzedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAnalysisId() { return analysisId; }
    public String getOriginalRequirement() { return originalRequirement; }
    public ScenarioType getScenarioType() { return scenarioType; }
    public String getScenarioRationale() { return scenarioRationale; }
    public List<String> getAmbiguities() { return ambiguities; }
    public List<EngineeringTask> getTasks() { return tasks; }
    public List<String> getRisks() { return risks; }
    public List<String> getAssumptions() { return assumptions; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }

    public static class Builder {
        private String analysisId;
        private String originalRequirement;
        private ScenarioType scenarioType;
        private String scenarioRationale;
        private List<String> ambiguities;
        private List<EngineeringTask> tasks;
        private List<String> risks;
        private List<String> assumptions;
        private LocalDateTime analyzedAt;

        public Builder analysisId(String analysisId) { this.analysisId = analysisId; return this; }
        public Builder originalRequirement(String originalRequirement) { this.originalRequirement = originalRequirement; return this; }
        public Builder scenarioType(ScenarioType scenarioType) { this.scenarioType = scenarioType; return this; }
        public Builder scenarioRationale(String scenarioRationale) { this.scenarioRationale = scenarioRationale; return this; }
        public Builder ambiguities(List<String> ambiguities) { this.ambiguities = ambiguities; return this; }
        public Builder tasks(List<EngineeringTask> tasks) { this.tasks = tasks; return this; }
        public Builder risks(List<String> risks) { this.risks = risks; return this; }
        public Builder assumptions(List<String> assumptions) { this.assumptions = assumptions; return this; }
        public Builder analyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; return this; }

        public RequirementAnalysis build() {
            return new RequirementAnalysis(this);
        }
    }
}
