package com.aiprototype.engine.controller;

import com.aiprototype.core.dto.AnalysisResponse;
import com.aiprototype.core.dto.RequirementRequest;
import com.aiprototype.engine.service.RequirementAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requirements")
@RequiredArgsConstructor
@Tag(name = "Requirement Engine",
     description = "Classify and decompose any software requirement into a structured engineering plan")
public class RequirementController {

    private final RequirementAnalyzerService analyzerService;

    @PostMapping("/analyze")
    @Operation(
            summary = "Analyze a software requirement",
            description = "Accepts any Greenfield, Brownfield, or Ambiguous requirement. " +
                          "Returns: scenario classification, task breakdown with AI prompt hints, " +
                          "identified ambiguities, risks, and assumptions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requirement successfully analyzed",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed — requirement is blank or too short",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "422", description = "Requirement text is structurally valid but cannot be processed",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody RequirementRequest request) {
        return ResponseEntity.ok(analyzerService.analyze(request));
    }

    @GetMapping("/health")
    @Operation(
            summary = "Engine health probe",
            description = "Returns OK when the engine is running"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Requirement Engine is running");
    }
}
