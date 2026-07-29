package com.aiprototype.core.domain;

import com.aiprototype.core.enums.ScenarioType;

public record ClassificationResult(ScenarioType type, String rationale) {}
