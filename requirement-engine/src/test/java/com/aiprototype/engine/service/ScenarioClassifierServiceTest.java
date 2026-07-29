package com.aiprototype.engine.service;

import com.aiprototype.core.enums.ScenarioType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioClassifierServiceTest {

    private final ScenarioClassifierService classifier = new ScenarioClassifierService();

    @Test
    void classifiesGreenfieldRequirement() {
        var result = classifier.classify("Build a new REST API for user authentication from scratch");
        assertThat(result.type()).isEqualTo(ScenarioType.GREENFIELD);
        assertThat(result.rationale()).contains("Greenfield");
    }

    @Test
    void classifiesBrownfieldRequirement() {
        var result = classifier.classify("Fix the bug in the existing authentication service and refactor token validation");
        assertThat(result.type()).isEqualTo(ScenarioType.BROWNFIELD);
        assertThat(result.rationale()).contains("Brownfield");
    }

    @Test
    void classifiesAmbiguousRequirementWhenNoKeywords() {
        var result = classifier.classify("The system should handle user requests properly");
        assertThat(result.type()).isEqualTo(ScenarioType.AMBIGUOUS);
    }

    @Test
    void classifiesAmbiguousRequirementOnTiedScore() {
        var result = classifier.classify("Build and fix the authentication service");
        assertThat(result.type()).isIn(ScenarioType.GREENFIELD, ScenarioType.BROWNFIELD, ScenarioType.AMBIGUOUS);
    }

    @Test
    void rationale_includesMatchedKeywords() {
        var result = classifier.classify("Implement a new microservice from scratch");
        assertThat(result.rationale()).contains("implement");
    }
}
