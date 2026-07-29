package com.aiprototype.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload containing the software requirement to be analysed")
public class RequirementRequest {

    @NotBlank(message = "Requirement must not be blank")
    @Size(min = 10, max = 5000, message = "Requirement must be between 10 and 5000 characters")
    @Schema(
            description = "The software requirement text — Greenfield, Brownfield, or Ambiguous",
            example = "Build a scalable URL shortener service with APIs, persistence, and analytics."
    )
    private String requirement;

    @Schema(
            description = "Optional context: existing tech stack, constraints, or background",
            example = "Existing Spring Boot 3.x service, Java 21, PostgreSQL"
    )
    private String context;
}
