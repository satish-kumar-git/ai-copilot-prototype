package com.aiprototype.engine.service;

import com.aiprototype.core.domain.ClassificationResult;
import com.aiprototype.core.enums.ScenarioType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScenarioClassifierService {

    private static final List<String> GREENFIELD_KEYWORDS = List.of(
            "build", "create", "new", "implement", "design", "develop",
            "from scratch", "architect", "establish", "launch", "introduce",
            "greenfield", "initialize", "bootstrap", "start", "prototype", "mvp"
    );

    private static final List<String> BROWNFIELD_KEYWORDS = List.of(
            "fix", "bug", "enhance", "add", "improve", "refactor",
            "migrate", "upgrade", "update", "modify", "change", "existing",
            "legacy", "maintain", "patch", "extend", "integrate", "replace",
            "deprecate", "optimize", "rewrite", "adjust"
    );

    public ClassificationResult classify(String requirement) {
        String lower = requirement.toLowerCase();

        List<String> greenfieldMatches = matchedKeywords(lower, GREENFIELD_KEYWORDS);
        List<String> brownfieldMatches = matchedKeywords(lower, BROWNFIELD_KEYWORDS);

        int greenfieldScore = greenfieldMatches.size();
        int brownfieldScore = brownfieldMatches.size();

        if (greenfieldScore > brownfieldScore) {
            return new ClassificationResult(
                    ScenarioType.GREENFIELD,
                    "Greenfield signals detected (%d): %s".formatted(greenfieldScore, String.join(", ", greenfieldMatches))
            );
        } else if (brownfieldScore > greenfieldScore) {
            return new ClassificationResult(
                    ScenarioType.BROWNFIELD,
                    "Brownfield signals detected (%d): %s".formatted(brownfieldScore, String.join(", ", brownfieldMatches))
            );
        } else {
            return new ClassificationResult(
                    ScenarioType.AMBIGUOUS,
                    "Ambiguous — equal keyword scores (greenfield=%d, brownfield=%d). Manual clarification required."
                            .formatted(greenfieldScore, brownfieldScore)
            );
        }
    }

    private List<String> matchedKeywords(String text, List<String> keywords) {
        return keywords.stream()
                .filter(text::contains)
                .collect(Collectors.toList());
    }
}
