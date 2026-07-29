package com.aiprototype.engine.service;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.Priority;
import com.aiprototype.core.domain.ScenarioType;
import com.aiprototype.core.domain.TaskCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskDecomposerService {

    public List<EngineeringTask> decompose(String requirement, ScenarioType scenarioType) {
        List<EngineeringTask> tasks = new ArrayList<>();
        tasks.add(requirementAnalysisTask());
        tasks.addAll(switch (scenarioType) {
            case GREENFIELD -> greenfieldTasks(requirement);
            case BROWNFIELD -> brownfieldTasks(requirement);
            case AMBIGUOUS  -> ambiguousTasks(requirement);
        });
        return tasks;
    }

    // ── T1 — shared across all scenarios ─────────────────────────────────────

    private EngineeringTask requirementAnalysisTask() {
        return task("T1",
                "Requirement Analysis & Clarification",
                "Identify all ambiguities, missing constraints, and undefined terms in the requirement before any design or implementation begins.",
                TaskCategory.CONFIGURATION, Priority.HIGH, List.of(),
                "List all ambiguities in this requirement. For each, provide a clarifying question and a recommended default assumption if the stakeholder is unavailable.");
    }

    // ── GREENFIELD T2–T11 ────────────────────────────────────────────────────

    private List<EngineeringTask> greenfieldTasks(String requirement) {
        return List.of(
                task("T2",
                        "Architecture & Design",
                        "Define the high-level system architecture: component boundaries, technology choices, communication patterns, and data flow.",
                        TaskCategory.API_DESIGN, Priority.HIGH, List.of("T1"),
                        "You are a solutions architect. For the requirement: [" + requirement + "], produce an ASCII component diagram showing all modules and their interactions, a technology stack table with rationale for each choice, and three Architecture Decision Records (ADRs) covering the most consequential design decisions."),

                task("T3",
                        "Project Scaffold",
                        "Generate the complete project skeleton: directory structure, build files, Spring Boot entry point, and base configuration.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T2"),
                        "You are a senior Java engineer. Scaffold a Spring Boot 3 + Java 21 Gradle multi-module project for: [" + requirement + "]. Generate settings.gradle.kts, each module's build.gradle.kts with correct dependencies, the @SpringBootApplication main class, application.yml with sensible defaults, and a .gitignore. Include Lombok and springdoc-openapi."),

                task("T4",
                        "Data Model & Flyway Migration",
                        "Design all JPA entities, their relationships, and produce versioned Flyway migration scripts to create the schema.",
                        TaskCategory.SCHEMA_DESIGN, Priority.HIGH, List.of("T2"),
                        "You are a data architect. Based on: [" + requirement + "], generate all JPA @Entity classes with correct @Column, @Id, @GeneratedValue, and relationship annotations (@OneToMany, @ManyToOne, etc.). Then produce Flyway migration scripts (V1__init_schema.sql) that create the corresponding tables, foreign keys, and indexes for expected query patterns."),

                task("T5",
                        "Repository Layer",
                        "Implement Spring Data JPA repositories with all required query methods and any necessary custom JPQL or native queries.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.MEDIUM, List.of("T4"),
                        "You are a senior Java engineer. For each entity in: [" + requirement + "], create a Spring Data JPA repository interface extending JpaRepository. Add derived query methods for the primary lookup patterns, and annotate complex queries with @Query using JPQL. Include pagination support where collections may be large."),

                task("T6",
                        "Service Layer",
                        "Implement all @Service classes containing business logic, validation, transaction management, and orchestration between repositories.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.HIGH, List.of("T5"),
                        "You are a senior Java engineer. Implement the @Service classes for: [" + requirement + "]. Each service method must be annotated with @Transactional where appropriate, throw domain-specific exceptions (e.g. ResourceNotFoundException) for error cases, use Lombok @Slf4j for structured logging, and delegate all persistence operations to the repository layer."),

                task("T7",
                        "REST Controller",
                        "Implement @RestController classes that expose the service layer via HTTP, with full request validation and OpenAPI documentation annotations.",
                        TaskCategory.API_DESIGN, Priority.HIGH, List.of("T6"),
                        "You are a senior Java engineer. Implement the @RestController classes for: [" + requirement + "]. Use @RequestMapping, @PostMapping, @GetMapping etc., annotate request DTOs with @Valid and @RequestBody, document every endpoint with springdoc @Operation and @ApiResponse annotations, and return ResponseEntity with appropriate HTTP status codes (201 for creates, 204 for deletes, 404 for not found)."),

                task("T8",
                        "Global Exception Handler & MDC Filter",
                        "Implement a @RestControllerAdvice that maps all domain and validation exceptions to structured error responses, plus an MDC servlet filter for request tracing.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T7"),
                        "You are a senior Java engineer. Create a @RestControllerAdvice class for: [" + requirement + "] that handles MethodArgumentNotValidException (400), ResourceNotFoundException (404), and Exception (500), each returning a consistent ErrorResponse record with timestamp, status, message, and path. Also implement an OncePerRequestFilter that injects a UUID correlation ID into MDC and the response header X-Correlation-Id."),

                task("T9",
                        "Unit Tests",
                        "Write JUnit 5 unit tests for the service layer and controller layer achieving at least 80% coverage.",
                        TaskCategory.TESTING, Priority.HIGH, List.of("T6", "T7"),
                        "You are a senior test engineer. Write JUnit 5 unit tests for the service and controller layers implementing: [" + requirement + "]. Use @ExtendWith(MockitoExtension.class) and Mockito for service tests; use @WebMvcTest with MockMvc for controller tests. Cover the happy path, validation errors, not-found cases, and any branching business logic. Assert both HTTP status codes and response body content."),

                task("T10",
                        "Configuration & Containerisation",
                        "Produce a multi-stage Dockerfile, docker-compose.yml for local development, and a GitHub Actions CI workflow.",
                        TaskCategory.CONFIGURATION, Priority.MEDIUM, List.of("T7"),
                        "You are a DevOps engineer. For the Spring Boot application implementing: [" + requirement + "], produce: a multi-stage Dockerfile using eclipse-temurin:21-jdk-alpine for build and eclipse-temurin:21-jre-alpine for runtime; a docker-compose.yml that wires the app to a PostgreSQL 16 container with health checks; and a GitHub Actions workflow (build → test → docker build → push to GHCR)."),

                task("T11",
                        "Documentation",
                        "Write a comprehensive README and ensure all REST endpoints are fully documented via springdoc OpenAPI annotations.",
                        TaskCategory.DOCUMENTATION, Priority.MEDIUM, List.of("T7", "T9"),
                        "You are a technical writer and Java engineer. For: [" + requirement + "], produce a README.md covering: project purpose, prerequisites, how to run locally (Gradle and Docker), environment variable reference, and API usage examples with curl snippets. Verify every @RestController method has @Operation(summary, description) and @ApiResponse entries so the Swagger UI at /swagger-ui.html is fully self-describing.")
        );
    }

    // ── BROWNFIELD T2–T7 ────────────────────────────────────────────────────

    private List<EngineeringTask> brownfieldTasks(String requirement) {
        return List.of(
                task("T2",
                        "Codebase Analysis",
                        "Examine the existing codebase to understand the current architecture, identify all components touched by this change, and surface hidden coupling.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T1"),
                        "You are a senior Java engineer performing codebase analysis for: [" + requirement + "]. Identify every class, method, and configuration file that will need to change. Map the call graph from the entry point down to the persistence layer. Flag any areas of technical debt (God classes, missing tests, hardcoded config) that will increase change risk."),

                task("T3",
                        "Impact Assessment",
                        "Document the full blast radius of the change: downstream consumers, API contract deltas, schema diffs, and performance implications.",
                        TaskCategory.SCHEMA_DESIGN, Priority.HIGH, List.of("T2"),
                        "You are a senior architect. Produce a structured impact assessment for: [" + requirement + "]. Include: a table of affected API endpoints with before/after contract diffs, any database schema changes required and their migration complexity, downstream services or clients that consume the changed interface, and a risk matrix (likelihood × severity) for each identified risk."),

                task("T4",
                        "Schema Migration",
                        "Produce versioned, rollback-safe Flyway migration scripts for any database schema changes required by this modification.",
                        TaskCategory.SCHEMA_DESIGN, Priority.HIGH, List.of("T3"),
                        "You are a database engineer. For the schema changes required by: [" + requirement + "], write Flyway migration scripts (V{n}__description.sql) that are safe to run on a live database. Use ADD COLUMN with defaults rather than NOT NULL without defaults, create indexes concurrently where supported, and provide a corresponding undo migration (U{n}__description.sql) for rollback."),

                task("T5",
                        "Implementation",
                        "Apply the planned changes to the codebase following the existing patterns, keeping the diff minimal and backward-compatible.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.HIGH, List.of("T4"),
                        "You are a senior Java engineer. Implement the changes for: [" + requirement + "] in the existing Spring Boot 3 codebase. Follow the established package structure and naming conventions. Where behaviour changes, retain the old method with @Deprecated and delegate to the new one to preserve backward compatibility. Add @Slf4j log statements at key decision points."),

                task("T6",
                        "Regression & New Tests",
                        "Write new tests covering the changed behaviour and run the full regression suite to confirm no existing functionality is broken.",
                        TaskCategory.TESTING, Priority.HIGH, List.of("T5"),
                        "You are a senior test engineer. For the changes in: [" + requirement + "], write JUnit 5 tests that cover both the new behaviour and the regression scenarios identified during impact assessment. Use @SpringBootTest with TestRestTemplate for integration tests touching the database. Produce a test coverage report and confirm the overall line coverage has not decreased."),

                task("T7",
                        "Documentation Update",
                        "Update all affected documentation: OpenAPI annotations, README, ADRs, and runbooks to reflect the new system state.",
                        TaskCategory.DOCUMENTATION, Priority.LOW, List.of("T5"),
                        "You are a technical writer. Update all documentation affected by: [" + requirement + "]. Revise OpenAPI @Operation and @ApiResponse annotations on changed endpoints, add an Architecture Decision Record capturing why this approach was chosen over alternatives, update the README's API usage examples, and revise the runbook's deployment section if environment variables or startup flags changed.")
        );
    }

    // ── AMBIGUOUS T2–T6 ─────────────────────────────────────────────────────

    private List<EngineeringTask> ambiguousTasks(String requirement) {
        return List.of(
                task("T2",
                        "Requirement Disambiguation",
                        "Engage stakeholders to resolve all identified ambiguities and produce a revised, unambiguous requirement statement with agreed acceptance criteria.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T1"),
                        "You are a senior product manager. Using the ambiguities identified in T1 for: [" + requirement + "], draft a structured stakeholder interview guide with one targeted question per ambiguity. For each question, provide a multiple-choice set of likely answers and the engineering implications of each option. End with a revised requirement template to be completed after the session."),

                task("T3",
                        "Hypothesis Formation",
                        "Based on available context, form and document two or three interpretations of the requirement to guide the investigation phase.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T2"),
                        "You are a senior engineer. Given the ambiguous requirement: [" + requirement + "], form three distinct interpretations ranked by likelihood. For each hypothesis state: the assumed intent, the technical approach it implies, the effort estimate (S/M/L), and the key risk. This document will be shared with stakeholders to accelerate disambiguation."),

                task("T4",
                        "Investigation & Profiling",
                        "Instrument the system, gather metrics, and analyse logs or traces to determine which hypothesis best matches observed system behaviour.",
                        TaskCategory.CONFIGURATION, Priority.HIGH, List.of("T3"),
                        "You are a senior observability engineer. For the investigation of: [" + requirement + "], produce: a list of metrics to capture (latency percentiles, error rates, throughput), the Micrometer/Actuator configuration to expose them, log query patterns using Logback MDC filters, and a profiling plan using async-profiler or JFR. Define the decision criteria that will confirm or reject each hypothesis from T3."),

                task("T5",
                        "Scoped Implementation",
                        "Implement the minimal change that addresses the validated hypothesis, behind a feature flag to allow safe production rollout.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.MEDIUM, List.of("T4"),
                        "You are a senior Java engineer. Implement the minimal code change that addresses the confirmed hypothesis for: [" + requirement + "]. Wrap the change in a feature flag (@ConditionalOnProperty or a simple boolean config) so it can be enabled per environment. Keep the change to the smallest number of files possible and annotate each modification with a comment referencing the hypothesis ID from T3."),

                task("T6",
                        "Measurement & Outcome Validation",
                        "Verify the implementation resolves the original problem by comparing before/after metrics against the success criteria defined in T4.",
                        TaskCategory.TESTING, Priority.HIGH, List.of("T5"),
                        "You are a senior engineer. Produce a validation report for: [" + requirement + "]. Compare the before and after metrics captured during T4, confirming each success criterion is met. Write a JUnit 5 test that asserts the target metric (e.g. response time, error rate) stays within the agreed threshold. Document the outcome and recommendation — close the hypothesis loop or escalate with new evidence.")
        );
    }

    // ── factory helper ───────────────────────────────────────────────────────

    private EngineeringTask task(String id, String title, String description,
                                  TaskCategory category, Priority priority,
                                  List<String> dependencies, String aiPromptHint) {
        return EngineeringTask.builder()
                .id(id)
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .dependencies(dependencies)
                .aiPromptHint(aiPromptHint)
                .build();
    }
}
