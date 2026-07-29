package com.aiprototype.engine.service;

import com.aiprototype.core.domain.ClassificationResult;
import com.aiprototype.core.domain.ScenarioType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ScenarioClassifierServiceTest {

    private final ScenarioClassifierService classifier = new ScenarioClassifierService();

    @Test
    void classify_greenfieldKeyword_returnsGreenfield() {
        ClassificationResult result = classifier.classify(
                "Build a new user authentication service from scratch");

        assertThat(result.type()).isEqualTo(ScenarioType.GREENFIELD);
        assertThat(result.rationale()).contains("construction language");
    }

    @Test
    void classify_brownfieldKeyword_returnsBrownfield() {
        ClassificationResult result = classifier.classify(
                "Fix the performance regression in the existing payment service");

        assertThat(result.type()).isEqualTo(ScenarioType.BROWNFIELD);
        assertThat(result.rationale()).contains("modification language");
    }

    @Test
    void classify_noKeywords_returnsAmbiguous() {
        ClassificationResult result = classifier.classify(
                "The system needs to handle more load");

        assertThat(result.type()).isEqualTo(ScenarioType.AMBIGUOUS);
    }

    @Test
    void classify_mixedEqualKeywords_returnsAmbiguous() {
        // "build" scores greenfield=1, "fix" scores brownfield=1 → tied → AMBIGUOUS
        ClassificationResult result = classifier.classify(
                "Build and fix the notification service");

        assertThat(result.type()).isEqualTo(ScenarioType.AMBIGUOUS);
        assertThat(result.rationale()).contains("equal Greenfield and Brownfield indicators");
    }

    @Test
    void classify_caseInsensitive_works() {
        ClassificationResult result = classifier.classify(
                "BUILD a scalable service");

        assertThat(result.type()).isEqualTo(ScenarioType.GREENFIELD);
    }

    @Test
    void classify_multipleGreenfieldKeywords_highScore() {
        // build, create, new, implement, design — 5 greenfield keywords, zero brownfield
        ClassificationResult result = classifier.classify(
                "Build and create a new system, implement and design from scratch");

        assertThat(result.type()).isEqualTo(ScenarioType.GREENFIELD);
    }
}
