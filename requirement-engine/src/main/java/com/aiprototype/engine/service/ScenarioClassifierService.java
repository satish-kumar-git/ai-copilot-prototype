package com.aiprototype.engine.service;

import com.aiprototype.core.domain.ClassificationResult;
import com.aiprototype.core.domain.ScenarioType;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ScenarioClassifierService {

    private static final Set<String> GREENFIELD_KEYWORDS = Set.of(
            "build", "create", "new", "implement", "design", "develop",
            "from scratch", "initial", "setup", "bootstrap", "scaffold",
            "start", "launch", "introduce"
    );

    private static final Set<String> BROWNFIELD_KEYWORDS = Set.of(
            "fix", "bug", "enhance", "add", "improve", "refactor",
            "migrate", "upgrade", "update", "extend", "modify", "change",
            "existing", "current", "legacy", "replace", "rewrite",
            "performance", "optimize", "scale up", "regression"
    );

    public ClassificationResult classify(String requirement) {
        String lower = requirement.toLowerCase();

        long greenfieldScore = GREENFIELD_KEYWORDS.stream()
                .filter(lower::contains)
                .count();

        long brownfieldScore = BROWNFIELD_KEYWORDS.stream()
                .filter(lower::contains)
                .count();

        if (greenfieldScore == 0 && brownfieldScore == 0) {
            return new ClassificationResult(
                    ScenarioType.AMBIGUOUS,
                    "No clear Greenfield or Brownfield indicators found — engineer must clarify scope before decomposing tasks"
            );
        }

        if (greenfieldScore > brownfieldScore) {
            return new ClassificationResult(
                    ScenarioType.GREENFIELD,
                    "Requirement uses construction language ('build', 'create', 'implement') indicating a new system or feature"
            );
        }

        if (brownfieldScore > greenfieldScore) {
            return new ClassificationResult(
                    ScenarioType.BROWNFIELD,
                    "Requirement uses modification language ('fix', 'enhance', 'refactor') indicating change to an existing system"
            );
        }

        return new ClassificationResult(
                ScenarioType.AMBIGUOUS,
                "Requirement has equal Greenfield and Brownfield indicators — engineer should clarify whether this is a new system or an enhancement"
        );
    }
}
