package com.aiprototype.engine.service;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.enums.Priority;
import com.aiprototype.core.enums.ScenarioType;
import com.aiprototype.core.enums.TaskCategory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskDecomposerService {

    public List<EngineeringTask> decompose(String requirement, ScenarioType scenarioType) {
        return switch (scenarioType) {
            case GREENFIELD -> greenfieldTasks(requirement);
            case BROWNFIELD -> brownfieldTasks(requirement);
            case AMBIGUOUS  -> ambiguousTasks();
        };
    }

    private List<EngineeringTask> greenfieldTasks(String requirement) {
        return List.of(
                task("T-01",
                        "Define system requirements and acceptance criteria",
                        "Gather, document, and validate all functional and non-functional requirements. Define measurable acceptance criteria in Given/When/Then format.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of(),
                        "You are a senior business analyst. Given this requirement: [" + requirement + "], produce: 5 functional requirements, 3 non-functional requirements with measurable SLAs, and 5 acceptance criteria in Given/When/Then format."),

                task("T-02",
                        "Design system architecture",
                        "Define the high-level component diagram, technology choices, communication patterns, and data flow between modules.",
                        TaskCategory.API_DESIGN, Priority.HIGH, List.of(),
                        "You are a solutions architect. Design the system architecture for: [" + requirement + "]. Produce: ASCII component diagram, module responsibility table, tech stack with rationale, and key architectural decision records (ADRs)."),

                task("T-03",
                        "Design domain model and database schema",
                        "Identify all domain entities, their relationships, and design the persistence schema with indexing strategy.",
                        TaskCategory.SCHEMA_DESIGN, Priority.HIGH, List.of("T-01", "T-02"),
                        "You are a data architect. Based on: [" + requirement + "], design: all domain entities with fields and types, ERD showing relationships, DDL schema, and indexing strategy for expected query patterns."),

                task("T-04",
                        "Define REST API contract",
                        "Specify all API endpoints, request/response schemas, HTTP status codes, and error response format.",
                        TaskCategory.API_DESIGN, Priority.HIGH, List.of("T-02"),
                        "You are an API designer. Based on: [" + requirement + "], produce a complete OpenAPI 3.1 specification including: all endpoints with path/method/params/request body/response schemas, standard error envelope, and authentication approach."),

                task("T-05",
                        "Implement core services and business logic",
                        "Build the primary service layer implementing all business rules derived from requirements.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.HIGH, List.of("T-03", "T-04"),
                        "You are a senior Java 21 engineer using Spring Boot 3. Implement the service layer for: [" + requirement + "]. Write: service interfaces, implementations with business logic, dependency injection wiring, and transaction boundaries."),

                task("T-06",
                        "Write unit tests",
                        "Achieve minimum 80% unit test coverage on service and domain logic using JUnit 5 and Mockito.",
                        TaskCategory.TESTING, Priority.MEDIUM, List.of("T-05"),
                        "You are a senior test engineer. Write JUnit 5 unit tests for the service layer implementing: [" + requirement + "]. Cover: happy path, edge cases, and error scenarios. Use Mockito for all external dependencies."),

                task("T-07",
                        "Write integration tests",
                        "Verify end-to-end API behaviour including persistence and any external service interactions.",
                        TaskCategory.TESTING, Priority.MEDIUM, List.of("T-05"),
                        "You are a senior test engineer. Write Spring Boot integration tests for: [" + requirement + "]. Use @SpringBootTest with MockMvc, test all REST endpoints, and verify expected database state changes."),

                task("T-08",
                        "Configure deployment and CI/CD pipeline",
                        "Set up Dockerfile, docker-compose, and GitHub Actions CI pipeline for build, test, and publish.",
                        TaskCategory.CONFIGURATION, Priority.MEDIUM, List.of("T-05"),
                        "You are a DevOps engineer. Create deployment configuration for a Spring Boot 3 app implementing: [" + requirement + "]. Produce: multi-stage Dockerfile, docker-compose.yml, and GitHub Actions workflow (build → test → push to registry).")
        );
    }

    private List<EngineeringTask> brownfieldTasks(String requirement) {
        return List.of(
                task("T-01",
                        "Analyse existing system and assess change impact",
                        "Review the existing codebase, identify all affected components, and document the change impact including downstream risks.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of(),
                        "You are a senior engineer performing impact analysis. For this change request: [" + requirement + "], produce: list of affected files/classes, dependency impact graph, data migration requirements (if any), and risk severity assessment."),

                task("T-02",
                        "Write regression test baseline",
                        "Capture and lock current behaviour with tests before making any changes to ensure no regressions are introduced.",
                        TaskCategory.TESTING, Priority.HIGH, List.of("T-01"),
                        "You are a senior test engineer. Before modifying the system for: [" + requirement + "], write regression tests documenting current behaviour. Prioritise integration tests over the affected API surfaces."),

                task("T-03",
                        "Design change and migration plan",
                        "Define the detailed change steps, data migration scripts (if needed), feature flag strategy, and rollback procedure.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of("T-01"),
                        "You are a solutions architect. Create a change plan for: [" + requirement + "]. Include: step-by-step implementation plan, data migration SQL (if schema changes involved), feature flag strategy, and rollback runbook."),

                task("T-04",
                        "Implement the required changes",
                        "Execute the planned changes to the existing codebase, preserving backward compatibility unless explicitly changed.",
                        TaskCategory.SERVICE_IMPLEMENTATION, Priority.HIGH, List.of("T-02", "T-03"),
                        "You are a senior Java developer. Implement the following change to an existing Spring Boot 3 system: [" + requirement + "]. Follow established patterns, preserve backward compatibility, and keep changes minimal and focused."),

                task("T-05",
                        "Refactor affected modules",
                        "Clean up technical debt in modules touched by the change without altering observable behaviour.",
                        TaskCategory.REFACTORING, Priority.MEDIUM, List.of("T-04"),
                        "You are a senior developer performing post-change refactoring for: [" + requirement + "]. Identify: code duplication introduced, violated SOLID principles, and simplification opportunities. Produce refactored code with explanations of each change."),

                task("T-06",
                        "Update API contracts",
                        "Revise OpenAPI specification, mark deprecated fields, and notify downstream consumers of contract changes.",
                        TaskCategory.API_DESIGN, Priority.MEDIUM, List.of("T-04"),
                        "You are an API designer. Update the OpenAPI specification for changes introduced by: [" + requirement + "]. Mark deprecated endpoints/fields, document new endpoints, and produce a CHANGELOG entry for API consumers."),

                task("T-07",
                        "Update technical documentation",
                        "Reflect all changes in architecture docs, README, and operational runbooks.",
                        TaskCategory.DOCUMENTATION, Priority.LOW, List.of("T-04", "T-06"),
                        "You are a technical writer. Update system documentation to reflect changes from: [" + requirement + "]. Update: README, architecture decision records (ADRs), API docs, and the deployment/operations runbook."),

                task("T-08",
                        "Run full regression suite and sign off",
                        "Execute the full test suite, perform performance benchmark comparison, and produce a sign-off report.",
                        TaskCategory.TESTING, Priority.HIGH, List.of("T-04", "T-05"),
                        "You are a QA engineer validating: [" + requirement + "]. Produce: test execution checklist, regression test pass/fail summary, performance benchmark delta report, and a sign-off document confirming no regressions.")
        );
    }

    private List<EngineeringTask> ambiguousTasks() {
        return List.of(
                task("T-01",
                        "Schedule stakeholder clarification session",
                        "Identify all stakeholders and schedule a session to resolve ambiguities before any implementation begins.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of(),
                        "You are a senior product manager. Generate a structured clarification agenda. Include: 5 targeted questions to resolve scope, a template for capturing decisions, and success criteria for the session."),

                task("T-02",
                        "Document ambiguity resolution outcomes",
                        "Record all decisions made during clarification, revise the requirement statement, and obtain stakeholder sign-off.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of("T-01"),
                        "You are a business analyst. Template for documenting clarification outcomes: original ambiguous requirement, each question asked, decision reached, revised requirement statement, and a stakeholder sign-off checklist."),

                task("T-03",
                        "Re-submit clarified requirement for analysis",
                        "Once all ambiguities are resolved, re-submit the updated requirement for full GREENFIELD or BROWNFIELD decomposition.",
                        TaskCategory.DOCUMENTATION, Priority.HIGH, List.of("T-02"),
                        "Re-run the AI Copilot /api/v1/requirements/analyze endpoint with the clarified requirement text to receive a complete task decomposition.")
        );
    }

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
