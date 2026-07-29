package com.aiprototype.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequirementRequest(
        @NotBlank(message = "Requirement must not be blank")
        @Size(min = 10, max = 5000, message = "Requirement must be between 10 and 5000 characters")
        String requirement,

        String context
) {}
