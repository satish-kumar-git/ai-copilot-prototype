# AI Copilot Prototype

A multi-module Java 21 / Spring Boot 3.3.5 backend that classifies free-text software requirements and decomposes them into ordered, AI-ready engineering task breakdowns.

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Module Structure](#3-module-structure)
4. [Tech Stack](#4-tech-stack)
5. [Classification Logic](#5-classification-logic)
6. [Task Categories](#6-task-categories)
7. [Prerequisites](#7-prerequisites)
8. [Getting Started](#8-getting-started)
9. [API Reference](#9-api-reference)
10. [Response Schema](#10-response-schema)
11. [Design Decisions](#11-design-decisions)
12. [Risk Register](#12-risk-register)
13. [Extending the System](#13-extending-the-system)
14. [Assumptions & Limitations](#14-assumptions--limitations)

---

## 1. Overview

Software engineers spend a significant portion of every sprint translating vague stakeholder requirements into concrete engineering tasks. This translation step is error-prone: ambiguous wording leads to wrong assumptions, missing non-functional requirements surface late, and there is no systematic way to ensure every new project asks the same foundational questions.

**AI Copilot Prototype** is a REST API that automates that translation step. Submit any free-text requirement and the engine returns a fully structured analysis: a scenario classification (Greenfield / Brownfield / Ambiguous), an ordered task breakdown with dependency chains, a list of detected ambiguities with remediation guidance, a risk register, and a baseline set of assumptions — all in a single JSON response.

Each task in the breakdown includes a self-contained `aiPromptHint`: a ready-to-paste prompt that an engineer can drop into any LLM to generate the implementation output for that specific task (scaffold, entity model, service layer, controller, tests). The `url-shortener` module in this repository demonstrates this workflow end-to-end: it was scaffolded directly from the `aiPromptHint` values produced by the requirement engine for a URL shortener requirement.

The system is designed for individual engineers, team leads, and AI-augmented development workflows where speed and consistency of task planning matter more than novelty. It requires no LLM API key, runs entirely offline, and produces deterministic output suitable for CI pipelines or pre-sprint automation.

---

## 2. System Architecture

```
                          ┌─────────────────────────────────────────────────┐
                          │           requirement-engine  (port 8090)        │
                          │                                                   │
  HTTP Client             │  ┌──────────────────────┐                        │
  POST /api/v1/           │  │  RequirementController│                        │
  requirements/analyze ──►│  │  @RestController      │                        │
                          │  └──────────┬───────────┘                        │
                          │             │ RequirementRequest                  │
                          │             ▼                                     │
                          │  ┌──────────────────────────┐                    │
                          │  │  RequirementAnalyzerService│                   │
                          │  │  @Service (orchestrator)  │                   │
                          │  └──────┬──────────┬─────────┘                   │
                          │         │          │                              │
                          │         ▼          ▼                              │
                          │  ┌────────────┐ ┌───────────────────┐            │
                          │  │ Scenario   │ │ TaskDecomposer    │            │
                          │  │ Classifier │ │ Service           │            │
                          │  │ Service    │ │                   │            │
                          │  │            │ │ GREENFIELD → T1–T11│           │
                          │  │ keyword    │ │ BROWNFIELD → T1–T7 │           │
                          │  │ scoring   │ │ AMBIGUOUS  → T1–T6 │           │
                          │  └─────┬──────┘ └────────┬──────────┘            │
                          │        │ ClassificationResult  │ List<EngineeringTask>│
                          │        └──────────┬───────────┘                  │
                          │                   │                               │
                          │                   ▼                               │
                          │  ┌──────────────────────────┐                    │
                          │  │  AnalysisResponse (JSON)  │                   │
                          │  │  analysisId, scenarioType,│                   │
                          │  │  tasks[], ambiguities[],  │                   │
                          │  │  risks[], assumptions[]   │                   │
                          │  └──────────────────────────┘                    │
                          │                   │                               │
                          └───────────────────┼───────────────────────────────┘
                                              │
                                              ▼
                                        HTTP Client
                                    ◄── 200 OK + JSON
```

The `core` module contains all shared domain classes (`EngineeringTask`, `RequirementAnalysis`, `AnalysisResponse`, enums). Neither `requirement-engine` nor `url-shortener` define any domain types — all shared contracts live exclusively in `core`.

---

## 3. Module Structure

| Module | Purpose | Spring Boot app | Port |
|--------|---------|:--------------:|------|
| `core` | Shared domain model, DTOs, enums — no Spring Boot, no persistence | No | — |
| `requirement-engine` | REST API: accepts requirements, classifies, decomposes, returns structured analysis | Yes | 8090 |
| `url-shortener` | Demo service scaffolded from `requirement-engine` output; shows the full Greenfield workflow | Yes | 8091 |

```
ai-copilot-prototype/
├── build.gradle.kts                  # root: plugin declarations only (apply false)
├── settings.gradle.kts               # module registry
├── gradle.properties                 # daemon=true, parallel=true, caching=true
├── Dockerfile                        # multi-stage: gradle:8.10-jdk21 → eclipse-temurin:21-jre-jammy
├── docker-compose.yml
├── .dockerignore
├── core/
│   └── src/main/java/com/aiprototype/core/
│       ├── domain/                   # EngineeringTask, RequirementAnalysis, ClassificationResult, enums
│       └── dto/                      # RequirementRequest, AnalysisResponse, TaskDto
├── requirement-engine/
│   └── src/main/java/com/aiprototype/engine/
│       ├── controller/               # RequirementController
│       ├── config/                   # OpenApiConfig
│       ├── exception/                # GlobalExceptionHandler, ErrorResponse, InvalidRequirementException
│       ├── mapper/                   # AnalysisMapper (utility, not used by current flow)
│       └── service/                  # RequirementAnalyzerService, ScenarioClassifierService, TaskDecomposerService
└── url-shortener/
    └── src/main/java/com/aiprototype/urlshortener/
        └── UrlShortenerApplication.java
```

---

## 4. Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Virtual threads (`--enable-preview` not required; Spring Boot 3.2+ opt-in) |
| Spring Boot | 3.3.5 | Web, Validation, Actuator, Data JPA auto-configuration |
| Gradle (Kotlin DSL) | 9.6.1 | Multi-module build, dependency resolution |
| Lombok | (BOM-managed) | `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor` |
| springdoc-openapi | 2.6.0 | Swagger UI at `/swagger-ui.html`, API docs at `/api-docs` |
| Jakarta Validation | (BOM-managed) | `@NotBlank`, `@Size` on request DTOs |
| Spring Boot Actuator | (BOM-managed) | `/actuator/health`, `/actuator/metrics` |
| H2 | (BOM-managed) | In-memory database for `url-shortener` prototype |
| PostgreSQL JDBC | (BOM-managed) | Production datasource for `url-shortener` |
| Flyway | (BOM-managed) | Schema versioning in `url-shortener` |
| JUnit 5 | (BOM-managed) | Unit and integration tests |
| Mockito | (BOM-managed) | Mock-based unit tests (`@ExtendWith(MockitoExtension.class)`) |
| Docker | 24+ | Multi-stage image build and Compose orchestration |
| eclipse-temurin | 21-jre-jammy | Minimal JRE runtime image |

---

## 5. Classification Logic

`ScenarioClassifierService` applies keyword scoring to the lowercased requirement text using two `Set<String>` constant sets (O(1) per lookup via `stream().filter().count()`).

### Greenfield keywords (14)

`build`, `create`, `new`, `implement`, `design`, `develop`, `from scratch`, `initial`, `setup`, `bootstrap`, `scaffold`, `start`, `launch`, `introduce`

### Brownfield keywords (21)

`fix`, `bug`, `enhance`, `add`, `improve`, `refactor`, `migrate`, `upgrade`, `update`, `extend`, `modify`, `change`, `existing`, `current`, `legacy`, `replace`, `rewrite`, `performance`, `optimize`, `scale up`, `regression`

### Decision tree

```
greenfieldScore = count of GREENFIELD_KEYWORDS found in lower(requirement)
brownfieldScore = count of BROWNFIELD_KEYWORDS found in lower(requirement)

both == 0          → AMBIGUOUS  ("No clear Greenfield or Brownfield indicators found")
green > brown      → GREENFIELD ("Requirement uses construction language")
brown > green      → BROWNFIELD ("Requirement uses modification language")
green == brown > 0 → AMBIGUOUS  ("Equal Greenfield and Brownfield indicators")
```

The approach is deliberately rule-based (no LLM call) to produce deterministic, low-latency, offline-capable results. See [Design Decisions](#11-design-decisions) for the rationale.

---

## 6. Task Categories

Every `EngineeringTask` is tagged with one of eight `TaskCategory` values:

| Category | Description |
|----------|-------------|
| `SCHEMA_DESIGN` | Database entity modelling, JPA annotations, Flyway migration scripts |
| `API_DESIGN` | REST endpoint design, OpenAPI annotations, request/response contracts |
| `SERVICE_IMPLEMENTATION` | Business logic, `@Service` classes, `@Transactional` orchestration |
| `TESTING` | JUnit 5 unit tests, `@WebMvcTest` controller tests, integration tests |
| `DOCUMENTATION` | README updates, OpenAPI descriptions, Architecture Decision Records |
| `CONFIGURATION` | Spring profiles, `application.yml`, Dockerfile, CI/CD pipelines, requirement analysis tasks |
| `REFACTORING` | Code restructuring without behaviour change (not generated by the current decomposer; reserved for future) |
| `BUG_FIX` | Targeted defect resolution (not generated by the current decomposer; reserved for future) |

---

## 7. Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 21 | Required to compile and run; use `JAVA_HOME` or Gradle toolchain |
| Docker | 24+ | Required only for `docker compose up`; not needed for local Gradle runs |
| Gradle | 8.10+ | Use the included `./gradlew` wrapper — no global install required |
| `curl` + `jq` | Any | Optional; used in the smoke-test examples below |

Verify your Java version:
```bash
java -version   # must print 21.x.x
./gradlew -v    # prints Gradle and JVM versions used by the build
```

---

## 8. Getting Started

### a. Clone & build

```bash
git clone <repo-url> ai-copilot-prototype
cd ai-copilot-prototype
./gradlew build
```

This compiles all modules, runs the 35-test suite, and produces:
- `requirement-engine/build/libs/requirement-engine.jar`
- `url-shortener/build/libs/url-shortener.jar`

### b. Run locally

```bash
./gradlew :requirement-engine:bootRun
```

The server starts on **http://localhost:8090**. Swagger UI is available at http://localhost:8090/swagger-ui.html.

### c. Run with Docker

```bash
docker compose up --build
```

This builds a multi-stage image (deps layer cached separately) and starts the service on port 8090 with a health check. First build takes ~3 minutes; subsequent builds use cached dependency layers.

```bash
docker compose down          # stop and remove containers
docker compose up -d         # start detached
docker compose logs -f       # stream logs
```

### d. Run tests

```bash
./gradlew test                              # all modules
./gradlew :requirement-engine:test          # engine only
./gradlew :requirement-engine:test --rerun  # force re-run (bypass cache)
```

Test report: `requirement-engine/build/reports/tests/test/index.html`

---

## 9. API Reference

### Endpoints

| Method | Path | Description | Request | Response | Status codes |
|--------|------|-------------|---------|----------|-------------|
| `POST` | `/api/v1/requirements/analyze` | Classify and decompose a requirement | `RequirementRequest` JSON | `AnalysisResponse` JSON | 200, 400, 422, 500 |
| `GET` | `/api/v1/requirements/health` | Engine liveness probe | — | `"Requirement Engine is running"` (plain text) | 200 |
| `GET` | `/actuator/health` | Spring Boot Actuator health | — | `{"status":"UP", ...}` | 200 |
| `GET` | `/swagger-ui.html` | Swagger UI (redirects to `/swagger-ui/index.html`) | — | HTML | 200 |
| `GET` | `/api-docs` | Raw OpenAPI 3.0 JSON spec | — | OpenAPI JSON | 200 |

### Request body — `RequirementRequest`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `requirement` | `String` | `@NotBlank`, `@Size(min=10, max=5000)` | The free-text software requirement |
| `context` | `String` | Optional | Additional context (stack, constraints, team size) |

### Error response — `ErrorResponse`

| Field | Type | Description |
|-------|------|-------------|
| `status` | `int` | HTTP status code |
| `error` | `String` | Machine-readable error code |
| `message` | `String` | Human-readable description |
| `path` | `String` | Request URI |
| `timestamp` | `String` | ISO-8601 datetime |
| `details` | `List<String>` | Field-level validation errors (400 only) |

### curl examples

**Greenfield**
```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a scalable URL shortener service with REST APIs, persistence, click analytics, and a custom alias feature.",
    "context": "Java 21, Spring Boot 3.x, H2 for prototype, PostgreSQL for production"
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length), firstTask: .tasks[0].id}'
# → {"type":"GREENFIELD","taskCount":11,"firstTask":"T1"}
```

**Brownfield**
```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Fix the performance regression in the existing payment service and refactor the legacy database connection pool"
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length)}'
# → {"type":"BROWNFIELD","taskCount":7}
```

**Ambiguous**
```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "The system needs to handle more load in certain conditions"
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length), ambiguities: .ambiguities}'
# → {"type":"AMBIGUOUS","taskCount":6,"ambiguities":[...]}
```

---

## 10. Response Schema

Annotated `AnalysisResponse` for a Greenfield requirement:

```jsonc
{
  // Unique UUID for this analysis run — use for logging and correlation
  "analysisId": "a3f2c1d4-9b8e-4f2a-b1c3-7d6e5f4a3b2c",

  // The original requirement text, echoed back verbatim
  "originalRequirement": "Build a scalable URL shortener service with REST APIs...",

  // GREENFIELD | BROWNFIELD | AMBIGUOUS
  "scenarioType": "GREENFIELD",

  // Human-readable explanation of why this type was assigned
  "scenarioRationale": "Requirement uses construction language ('build', 'create', 'implement') indicating a new system or feature",

  // Detected vague qualifiers with guidance on making them measurable
  "ambiguities": [
    "'scalable' is undefined — specify: expected requests/second, concurrent users, and data volume at peak",
    "Availability and SLA requirements are not stated"
  ],

  // Ordered task breakdown; T1 is always first (shared across all scenario types)
  "tasks": [
    {
      "id": "T1",
      "title": "Requirement Analysis & Clarification",
      "description": "Identify all ambiguities, missing constraints, and undefined terms...",
      "category": "CONFIGURATION",   // TaskCategory enum as string
      "priority": "HIGH",            // HIGH | MEDIUM | LOW
      "dependencies": [],            // IDs of tasks that must complete first
      // Self-contained prompt — paste directly into an LLM to execute this task
      "aiPromptHint": "List all ambiguities in this requirement. For each, provide a clarifying question..."
    },
    {
      "id": "T2",
      "title": "Architecture & Design",
      "category": "API_DESIGN",
      "priority": "HIGH",
      "dependencies": ["T1"],
      "aiPromptHint": "You are a solutions architect. For the requirement: [...], produce an ASCII component diagram..."
    }
    // T3–T11 follow for GREENFIELD (T2–T7 for BROWNFIELD, T2–T6 for AMBIGUOUS)
  ],

  // Engineering risks for this scenario type + 2 always-present AI-specific risks
  "risks": [
    "DESIGN: Over-engineering early — build for current validated requirements, not speculative future scale",
    "SECURITY: No authentication specified — public API is a production security risk",
    "AI_HALLUCINATION: AI-generated code may reference non-existent API methods or packages — verify each import against library Javadoc",
    "AI_COVERAGE_GAP: AI-generated tests often miss edge cases — review each test method and add: null inputs, boundary values, exception paths"
  ],

  // Baseline assumptions the analysis was made under
  "assumptions": [
    "Java 21 + Spring Boot 3.x is the target stack",
    "H2 in-memory database is acceptable for the prototype phase",
    "Authentication and authorisation are out of scope for the initial delivery",
    "All configuration is externalised via environment variables or application.yml",
    "Local development environment uses Docker Compose; Kubernetes is out of scope"
  ],

  // ISO-8601 timestamp of when this analysis was performed
  "analyzedAt": "2026-07-29T14:30:00.123456789"
}
```

### Task counts by scenario type

| Scenario | T1 (shared) | Scenario-specific | Total |
|----------|:-----------:|:-----------------:|:-----:|
| GREENFIELD | 1 | T2–T11 (10) | **11** |
| BROWNFIELD | 1 | T2–T7 (6) | **7** |
| AMBIGUOUS | 1 | T2–T6 (5) | **6** |

---

## 11. Design Decisions

| Decision | Alternatives considered | Rationale |
|----------|------------------------|-----------|
| **Keyword scoring for classification** | LLM-based classification (GPT-4, Claude) | Deterministic, zero-latency, no API key, no network dependency, fully testable with fixed inputs. LLM classification adds non-determinism and external cost; it can be layered on top of this baseline in a future version. |
| **Multi-module Gradle (core + engine + url-shortener)** | Single-module monolith; separate Git repos | Forces explicit API contracts between modules via `:core` dependency. `core` has no Spring Boot dependency — any JVM consumer can use the domain types. Separate repos add too much CI overhead for a prototype. |
| **No persistence in requirement-engine** | Store analyses in PostgreSQL for history/audit | Analysis is stateless and fast (<5ms). Adding a database introduces infra complexity with no user-facing benefit in the prototype. Persistence belongs in a future `analysis-history` module. |
| **Rule-based ambiguity detection** | NLP / entity extraction; regex | Six deterministic pattern checks (scalable, performance, no-auth, no-db, improve/enhance, no-SLA) cover the most common ambiguity classes with zero dependencies. The patterns are readable, testable, and extensible without a model. |
| **`aiPromptHint` per task** | Link to external prompt library; generic hints | Each hint is self-contained and interpolates the requirement text at decomposition time. Engineers can paste a single field directly into an LLM without any additional context assembly. |
| **`@RequiredArgsConstructor` + `@Service`** | Manual constructors; `@Autowired` field injection | Constructor injection is testable without Spring context (`new Service(mockDep)` works in unit tests). Lombok eliminates the boilerplate. |
| **`Set<String>` for keyword lookup** | `List<String>` with `contains()` | `Set.contains()` is O(1) vs O(n) for `List.contains()`. With 35 keywords checked against arbitrary text lengths, the difference is measurable at scale. |
| **`ErrorResponse` with `int status` field** | String status (`"BAD_REQUEST"`); no body on errors | Numeric status allows client-side switch/case without string parsing. The `details[]` array on 400 responses gives per-field validation errors in one pass. |

---

## 12. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|:----------:|:------:|------------|
| Keyword scorer misclassifies mixed-language requirements | Medium | Medium | AMBIGUOUS fallback on tied scores; per-request rationale string allows engineer to override manually |
| AI-generated code from `aiPromptHint` contains hallucinated imports | High | Medium | Each hint includes the instruction "verify each import against library Javadoc"; `AI_HALLUCINATION` risk is always surfaced in the response |
| AI-generated tests miss edge cases | High | Medium | `AI_COVERAGE_GAP` risk is always surfaced; engineers are reminded to add null, boundary, and exception-path tests |
| Requirement text contains sensitive data (PII, credentials) | Low | High | Engine is stateless and logs only character counts, never requirement text; deploy behind an API gateway with TLS in production |
| Greenfield keyword set is incomplete for domain-specific language | Medium | Low | `Set<String>` is a `private static final` constant — extending it requires a one-line code change and a unit test update |
| `url-shortener` module ships with H2 in production | Low | High | `application.yml` uses `${APP_BASE_URL:...}` pattern; production profiles must override datasource URL to PostgreSQL — H2 console is enabled only in default profile |
| Docker image runs as root | Low | High | `Dockerfile` creates `appuser`/`appgroup` and sets `USER appuser` before `ENTRYPOINT` |
| Gradle build cache serves stale compiled classes | Low | Low | `./gradlew clean build` always produces a fresh output; CI pipelines should run with `--no-build-cache` on release builds |

---

## 13. Extending the System

Adding a new engine module (e.g. `notification-engine`) takes four steps:

**Step 1 — Register in `settings.gradle.kts`**
```kotlin
include("core", "requirement-engine", "url-shortener", "notification-engine")
```

**Step 2 — Create `notification-engine/build.gradle.kts`**

Copy `url-shortener/build.gradle.kts` and adjust:
```kotlin
// Remove data-jpa, flyway, h2, postgresql if not needed
// Add module-specific dependencies
tasks.bootJar { archiveFileName = "notification-engine.jar" }
```
The `:core` dependency gives the new module immediate access to `RequirementRequest`, `AnalysisResponse`, `EngineeringTask`, and all domain types — no copy-paste required.

**Step 3 — Set port and create `application.yml`**

Follow the sequential port convention:

| Module | Port |
|--------|------|
| requirement-engine | 8090 |
| url-shortener | 8091 |
| notification-engine | 8092 |
| code-generator-engine | 8093 |

**Step 4 — Add service block to `docker-compose.yml`**
```yaml
  notification-engine:
    build:
      context: .
      dockerfile: notification-engine/Dockerfile
    ports:
      - "8092:8092"
    environment:
      - SPRING_APPLICATION_NAME=notification-engine
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8092/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    restart: unless-stopped
```

No changes to the `core` module are required unless you need new shared domain types, in which case add them to `core/src/main/java/com/aiprototype/core/domain/` and all modules get them on the next build.

---

## 14. Assumptions & Limitations

### Assumptions

- Requirements are written in English. The keyword scorer does not support other languages.
- The caller is a trusted internal client. No authentication or rate-limiting is implemented in the engine itself; these belong at the API gateway layer.
- A "requirement" is a single coherent statement (10–5000 characters). Multi-requirement batch processing is out of scope.
- H2 in-memory storage is acceptable for the `url-shortener` prototype. All data is lost on restart. A PostgreSQL datasource URL must be provided via the `SPRING_DATASOURCE_URL` environment variable for persistent deployments.

### Limitations

- **No persistence**: `requirement-engine` does not store analyses. Each call is stateless. If you need history, query logs or add a storage module.
- **No authentication**: The `/api/v1/requirements/analyze` endpoint is unauthenticated. Do not expose it directly on the public internet.
- **Keyword classifier only**: Classification accuracy depends entirely on the keyword sets. Requirements that use domain-specific or non-standard vocabulary may be misclassified. The AMBIGUOUS path is the safety net.
- **English only**: All keyword matching and fixed rationale strings are in English.
- **Task count is fixed per scenario**: GREENFIELD always produces 11 tasks, BROWNFIELD 7, AMBIGUOUS 6. There is no dynamic task generation based on requirement complexity or length.
- **`aiPromptHint` is a template, not an execution**: The hints interpolate the requirement text but do not call any LLM. They must be pasted manually into an LLM by the engineer.
- **`url-shortener` is a scaffold, not a complete service**: It has a `build.gradle.kts`, `application.yml`, and `@SpringBootApplication` entry point. The entity, Flyway migrations, repository, service, and controller are left for the engineer to generate using the T4–T7 `aiPromptHint` values from a GREENFIELD analysis of the URL shortener requirement.
