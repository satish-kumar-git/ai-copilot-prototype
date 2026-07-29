# AI Copilot Prototype

A multi-module Java 21 / Spring Boot 3.3.5 backend that demonstrates AI-assisted software development. The `requirement-engine` classifies free-text requirements and produces ordered, AI-ready task breakdowns with prompt hints. The `url-shortener` is a fully working service built end-to-end using those prompt hints — showing the complete AI-assisted workflow from requirement to production-quality code.

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Module Structure](#3-module-structure)
4. [Tech Stack](#4-tech-stack)
5. [AI-Assisted Development Workflow](#5-ai-assisted-development-workflow)
6. [Assignment Alignment](#6-assignment-alignment)
7. [Example Scenarios](#7-example-scenarios)
8. [Prerequisites](#8-prerequisites)
9. [Getting Started](#9-getting-started)
10. [Requirement Engine API Reference](#10-requirement-engine-api-reference)
11. [URL Shortener API Reference](#11-url-shortener-api-reference)
12. [Response Schemas](#12-response-schemas)
13. [Classification Logic](#13-classification-logic)
14. [Base62 Encoding](#14-base62-encoding)
15. [CORS Configuration](#15-cors-configuration)
16. [Design Decisions](#16-design-decisions)
17. [Testing](#17-testing)
18. [Risk Register](#18-risk-register)
19. [Extending the System](#19-extending-the-system)
20. [Assumptions & Limitations](#20-assumptions--limitations)

---

## 1. Overview

Software engineers spend a significant portion of every sprint translating vague stakeholder requirements into concrete engineering tasks. This translation step is error-prone: ambiguous wording leads to wrong assumptions, missing non-functional requirements surface late, and there is no systematic way to ensure every new project asks the same foundational questions.

**AI Copilot Prototype** solves this with two collaborating services:

**`requirement-engine` (port 8090)** — A REST API that accepts any free-text requirement and returns a fully structured analysis: scenario classification (Greenfield / Brownfield / Ambiguous), an ordered task breakdown with dependency chains, a list of detected ambiguities with remediation guidance, a risk register, and a baseline set of assumptions — all in a single deterministic JSON response. Each task includes a self-contained `aiPromptHint`: a ready-to-paste prompt for an LLM to generate the implementation artifact for that specific task.

**`url-shortener` (port 8091)** — A fully working URL shortening service built end-to-end by feeding the requirement engine's `aiPromptHint` values into an LLM. It demonstrates the complete AI-assisted development loop: requirement → classification → task breakdown → LLM-generated code → engineer-reviewed implementation → tested, deployable service. The service supports custom aliases, expiry dates, soft-delete, click tracking with per-day analytics, and Base62 short code generation.

The workflow embodies the assignment's core principle: **AI assists the engineer within tasks; the engineer owns execution and quality.**

---

## 2. System Architecture

```
  HTTP Client                                       HTTP Client
  POST /api/v1/requirements/analyze                 POST /api/v1/urls
        │                                                  │
        ▼                                                  ▼
┌────────────────────────────────┐        ┌────────────────────────────────────┐
│   requirement-engine (8090)    │        │     url-shortener (8091)           │
│                                │        │                                    │
│  RequirementController         │        │  UrlShortenerController            │
│       │                        │        │    POST /api/v1/urls     → 201     │
│  RequirementAnalyzerService    │        │    GET  /{shortCode}     → 302     │
│    ├── ScenarioClassifier      │        │    GET  /api/v1/urls/{c} → 200     │
│    │   keyword scoring         │        │    GET  /api/v1/urls/{c}/stats→200 │
│    └── TaskDecomposer          │        │    DELETE /api/v1/urls/{c}→ 204    │
│        GREENFIELD → T1–T11     │        │       │                            │
│        BROWNFIELD → T1–T7      │        │  UrlShortenerService               │
│        AMBIGUOUS  → T1–T6      │        │    Base62Encoder (6-char codes)    │
│                                │        │    ThreadLocalRandom (collision-   │
│  AnalysisResponse              │        │    resistant, retry up to 10x)     │
│    .tasks[].aiPromptHint ──────┼──────► │    Atomic click count @Modifying  │
│    .risks[]                    │        │    Soft-delete (active=false)      │
│    .ambiguities[]              │        │       │                            │
│    .assumptions[]              │        │  Repositories (Spring Data JPA)    │
└────────────────────────────────┘        │    ShortUrlRepository              │
                                          │    ClickEventRepository            │
        core (shared domain)              │       │                            │
  ┌──────────────────────────┐            │  H2 in-memory + Flyway migrations  │
  │  EngineeringTask         │            │    V1__create_short_urls.sql       │
  │  RequirementAnalysis     │            │    V2__create_click_events.sql     │
  │  ClassificationResult    │            └────────────────────────────────────┘
  │  TaskCategory (enum)     │
  │  ScenarioType (enum)     │
  └──────────────────────────┘
```

The `core` module contains all shared domain classes. Neither `requirement-engine` nor `url-shortener` define shared domain types — all contracts live in `core`.

---

## 3. Module Structure

| Module | Purpose | Spring Boot | Port |
|--------|---------|:-----------:|------|
| `core` | Shared domain model, DTOs, enums — no Spring Boot, no persistence | No | — |
| `requirement-engine` | REST API: classifies requirements and produces task breakdowns with AI prompt hints | Yes | 8090 |
| `url-shortener` | Full working URL shortener built from requirement-engine output; demonstrates the AI-assisted Greenfield workflow | Yes | 8091 |

```
ai-copilot-prototype/
├── build.gradle.kts                  # root: plugin declarations only (apply false)
├── settings.gradle.kts               # include("core","requirement-engine","url-shortener")
├── gradle.properties                 # daemon, parallel, caching, org.gradle.java.home
├── gradle/wrapper/
│   └── gradle-wrapper.properties    # distributionUrl=gradle-8.12-bin.zip
├── Dockerfile                        # multi-stage: gradle:8.10-jdk21 → eclipse-temurin:21-jre
├── docker-compose.yml
├── .dockerignore
├── prompts/
│   └── session.html                  # browser-based session tracker (15 prompt levels)
├── core/
│   └── src/main/java/com/aiprototype/core/
│       ├── domain/                   # EngineeringTask, RequirementAnalysis, ClassificationResult, enums
│       └── dto/                      # RequirementRequest, AnalysisResponse, TaskDto
├── requirement-engine/
│   └── src/main/java/com/aiprototype/engine/
│       ├── config/                   # OpenApiConfig, CorsConfig
│       ├── controller/               # RequirementController
│       ├── exception/                # GlobalExceptionHandler, ErrorResponse, InvalidRequirementException
│       ├── mapper/                   # AnalysisMapper
│       └── service/                  # RequirementAnalyzerService, ScenarioClassifierService, TaskDecomposerService
└── url-shortener/
    ├── src/main/java/com/aiprototype/urlshortener/
    │   ├── config/                   # CorsConfig, OpenApiConfig
    │   ├── controller/               # UrlShortenerController
    │   ├── domain/                   # ShortUrl, ClickEvent (JPA entities)
    │   ├── dto/                      # ShortenRequest, ShortenResponse, UrlStatsResponse
    │   ├── exception/                # GlobalExceptionHandler, UrlNotFoundException,
    │   │                             #   UrlAlreadyExistsException, ErrorResponse
    │   ├── repository/               # ShortUrlRepository, ClickEventRepository
    │   └── service/                  # UrlShortenerService, Base62Encoder
    └── src/main/resources/
        ├── application.yml
        └── db/migration/
            ├── V1__create_short_urls.sql
            └── V2__create_click_events.sql
```

---

## 4. Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Virtual threads (`spring.threads.virtual.enabled=true`) |
| Spring Boot | 3.3.5 | Web, Validation, Actuator, Data JPA |
| Gradle (Kotlin DSL) | 8.12 | Multi-module build (wrapper: `./gradlew`) |
| Lombok | BOM-managed | `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor` |
| springdoc-openapi | 2.6.0 | Swagger UI (`/swagger-ui.html`), OpenAPI spec (`/api-docs`) |
| Jakarta Validation | BOM-managed | `@NotBlank`, `@Size`, `@Pattern`, `@Future` on request DTOs |
| Spring Boot Actuator | BOM-managed | `/actuator/health`, `/actuator/metrics` |
| H2 | BOM-managed | In-memory database for `url-shortener` prototype |
| Flyway | BOM-managed | Schema versioning — V1 (short_urls) + V2 (click_events) |
| JUnit 5 | BOM-managed | Unit and integration tests |
| Mockito | BOM-managed | Mock-based unit tests (`@ExtendWith(MockitoExtension.class)`) |
| Docker | 24+ | Multi-stage image build and Compose orchestration |
| eclipse-temurin | 21-jre-jammy | Minimal JRE runtime image |

> **Gradle version note**: Spring Boot 3.3.5 + `io.spring.dependency-management` 1.1.6 require Gradle 7.5–8.x. Always use `./gradlew` (wrapper) — never the system `gradle` binary, which may be version 9.x and will fail to compile.

---

## 5. AI-Assisted Development Workflow

This project demonstrates the full AI-assisted development loop. The requirement engine is not just a utility — it was the actual tool used to plan and build the `url-shortener` service.

### Step 1: Submit the requirement

```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a scalable URL shortener service with REST APIs, persistence, click analytics, and a custom alias feature.",
    "context": "Java 21, Spring Boot 3.x, H2 for prototype, PostgreSQL for production"
  }'
```

### Step 2: Engine classifies as GREENFIELD and returns 11 tasks

The engine detected `build`, `scalable`, `implement` — **GREENFIELD**. The response included:

| Task | Title | Category | aiPromptHint used to build |
|------|-------|----------|---------------------------|
| T1 | Requirement Analysis & Clarification | CONFIGURATION | Identified: expiry, analytics, alias constraints |
| T2 | Architecture & Design | API_DESIGN | Designed module structure, entity model, port 8091 |
| T3 | Database Schema Design | SCHEMA_DESIGN | Generated `V1__create_short_urls.sql`, `V2__create_click_events.sql` |
| T4 | Core Entity & Repository | SERVICE_IMPLEMENTATION | Generated `ShortUrl`, `ClickEvent`, `ShortUrlRepository` |
| T5 | Service Layer Implementation | SERVICE_IMPLEMENTATION | Generated `UrlShortenerService`, `Base62Encoder` |
| T6 | REST Controller Implementation | API_DESIGN | Generated `UrlShortenerController` with all 6 endpoints |
| T7 | Exception Handling | API_DESIGN | Generated `GlobalExceptionHandler`, custom exceptions |
| T8 | Unit Tests | TESTING | Generated `UrlShortenerServiceTest` (10 tests) |
| T9 | Controller Tests | TESTING | Generated `UrlShortenerControllerTest` (8 tests) |
| T10 | Integration Tests | TESTING | Generated `UrlShortenerIntegrationTest` (5 tests) |
| T11 | Documentation & Configuration | DOCUMENTATION | Generated `application.yml`, CORS config, README |

### Step 3: Engineer reviews, adapts, and owns the output

Each LLM-generated artifact was reviewed by the engineer before committing:
- Changed read-modify-write click counting to `@Modifying @Query` to prevent lost updates under concurrent load
- Added regex constraint `[A-Za-z0-9_-]{3,20}` on `/{shortCode}` to prevent redirect route shadowing API routes
- Added `NON_KEYWORDS=VALUE` to H2 JDBC URL after a reserved-word conflict was discovered at runtime
- Enforced `allowCredentials(false)` when origin pattern is wildcard (browser spec requirement that LLM missed)

**The AI generated structure and boilerplate; the engineer caught the concurrency bug, the routing conflict, and the database compatibility issue.**

---

## 6. Assignment Alignment

| Assignment Requirement | How This Prototype Satisfies It |
|------------------------|--------------------------------|
| Design and implement a working prototype | Two running Spring Boot services (8090, 8091) with full test coverage |
| Demonstrate AI-assisted development | Requirement engine produces `aiPromptHint` per task; url-shortener was built using those hints |
| Engineer owns execution and quality | All LLM output reviewed; 4 non-obvious bugs caught and fixed by engineer review |
| Three example scenarios (Greenfield / Brownfield / Ambiguous) | Section 7 of this README with live curl examples |
| Architecture overview with AI tool integration | Section 2 diagram; Section 5 workflow table |
| Risk register including AI-specific risks | Section 18 — `AI_HALLUCINATION` and `AI_COVERAGE_GAP` are surfaced in every analysis response |
| Validation and QA | 54-test suite across 3 layers (unit, `@WebMvcTest` controller, `@SpringBootTest` integration) |
| End-to-end URL shortener implementation | Full CRUD, redirect, click analytics, custom alias, expiry, soft-delete |
| Assumptions and limitations | Section 20 |

---

## 7. Example Scenarios

### Greenfield — new system

```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a scalable URL shortener service with REST APIs, persistence, click analytics, and a custom alias feature.",
    "context": "Java 21, Spring Boot 3.x, H2 for prototype, PostgreSQL for production"
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length), firstHint: .tasks[0].aiPromptHint[:80]}'
# → {"type":"GREENFIELD","taskCount":11,"firstHint":"List all ambiguities in this re..."}
```

**Why GREENFIELD**: `build`, `scalable`, `implement` score higher than Brownfield keywords. Engine returns 11 tasks (T1–T11) covering design through documentation.

**Ambiguities surfaced**: `scalable` is undefined (requests/second? concurrent users?), no authentication specified, SLA not stated.

---

### Brownfield — modify an existing system

```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Fix the performance regression in the existing payment service and refactor the legacy database connection pool."
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length), risks: .risks[:2]}'
# → {"type":"BROWNFIELD","taskCount":7}
```

**Why BROWNFIELD**: `fix`, `performance`, `existing`, `refactor`, `legacy` outscore Greenfield keywords. Engine returns 7 tasks: analysis, impact assessment, targeted fix, regression tests, load testing, rollback plan, documentation.

---

### Ambiguous — unclear intent

```bash
curl -s -X POST http://localhost:8090/api/v1/requirements/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "The system needs to handle more load in certain conditions."
  }' | jq '{type: .scenarioType, taskCount: (.tasks | length), ambiguities: .ambiguities}'
# → {"type":"AMBIGUOUS","taskCount":6,"ambiguities":["Performance/scalability terms detected..."]}
```

**Why AMBIGUOUS**: No Greenfield or Brownfield keywords found. Engine returns 6 tasks focused on clarification, measurement, and de-risking before any implementation begins.

---

## 8. Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 21 | Required to compile and run |
| Docker | 24+ | Required only for `docker compose up` |
| Gradle wrapper | 8.12 | Use `./gradlew` — never the system `gradle` binary |
| `curl` + `jq` | Any | Optional; used in examples |

Verify your environment:
```bash
java -version         # must print 21.x.x
./gradlew -v          # prints Gradle 8.12 and JVM 21
```

**IDE setup (IntelliJ IDEA)**:
1. Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Gradle distribution: **Wrapper**
3. Gradle JVM: **Project SDK (Java 21)**
4. Click "Reload All Gradle Projects"

---

## 9. Getting Started

### a. Clone & build all modules

```bash
git clone <repo-url> ai-copilot-prototype
cd ai-copilot-prototype
./gradlew build
```

Compiles all 3 modules, runs all 54 tests, and produces:
- `requirement-engine/build/libs/requirement-engine.jar`
- `url-shortener/build/libs/url-shortener.jar`

### b. Run the requirement engine

```bash
./gradlew :requirement-engine:bootRun
# Listening on http://localhost:8090
# Swagger UI: http://localhost:8090/swagger-ui.html
```

### c. Run the URL shortener

```bash
./gradlew :url-shortener:bootRun
# Listening on http://localhost:8091
# Swagger UI: http://localhost:8091/swagger-ui.html
# H2 Console: http://localhost:8091/h2-console
#   JDBC URL: jdbc:h2:mem:urlshortener
#   Username: sa  Password: (empty)
```

### d. Run both services simultaneously

Open two terminals:
```bash
# Terminal 1
./gradlew :requirement-engine:bootRun

# Terminal 2
./gradlew :url-shortener:bootRun
```

### e. Run with Docker

```bash
docker compose up --build
# requirement-engine on port 8090
# url-shortener on port 8091
```

```bash
docker compose down          # stop and remove
docker compose up -d         # start detached
docker compose logs -f       # stream logs
```

### f. Run tests

```bash
./gradlew test                             # all modules (54 tests)
./gradlew :requirement-engine:test         # 29 tests
./gradlew :url-shortener:test              # 25 tests
./gradlew :requirement-engine:test --rerun # force re-run (bypass cache)
```

Test reports:
- `requirement-engine/build/reports/tests/test/index.html`
- `url-shortener/build/reports/tests/test/index.html`

---

## 10. Requirement Engine API Reference

### Endpoints

| Method | Path | Description | Status codes |
|--------|------|-------------|-------------|
| `POST` | `/api/v1/requirements/analyze` | Classify and decompose a requirement | 200, 400, 422, 500 |
| `GET` | `/api/v1/requirements/health` | Liveness probe | 200 |
| `GET` | `/actuator/health` | Spring Boot Actuator health | 200 |
| `GET` | `/swagger-ui.html` | Swagger UI | 200 |
| `GET` | `/api-docs` | Raw OpenAPI 3.0 JSON | 200 |

### Request — `RequirementRequest`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `requirement` | `String` | `@NotBlank`, `@Size(min=10, max=5000)` | Free-text software requirement |
| `context` | `String` | Optional | Additional context (stack, constraints, team size) |

### Error response — `ErrorResponse`

| Field | Type | Description |
|-------|------|-------------|
| `status` | `int` | HTTP status code |
| `error` | `String` | Machine-readable code |
| `message` | `String` | Human-readable description |
| `path` | `String` | Request URI |
| `timestamp` | `String` | ISO-8601 datetime |
| `details` | `List<String>` | Field-level validation errors (400 only) |

---

## 11. URL Shortener API Reference

Base URL: `http://localhost:8091`

### Endpoints

| Method | Path | Description | Request body | Response | Status |
|--------|------|-------------|-------------|----------|--------|
| `POST` | `/api/v1/urls` | Shorten a URL | `ShortenRequest` JSON | `ShortenResponse` JSON | 201, 400, 409 |
| `GET` | `/{shortCode}` | Redirect to original URL | — | `Location` header | 302, 404, 410 |
| `GET` | `/api/v1/urls/{shortCode}` | Get URL details | — | `ShortenResponse` JSON | 200, 404 |
| `GET` | `/api/v1/urls/{shortCode}/stats` | Get click analytics | — | `UrlStatsResponse` JSON | 200, 404 |
| `DELETE` | `/api/v1/urls/{shortCode}` | Soft-delete a URL | — | — | 204, 404 |
| `GET` | `/api/v1/urls/health` | Liveness probe | — | plain text | 200 |

`{shortCode}` must match `[A-Za-z0-9_-]{3,20}` (regex constraint prevents route shadowing).

### Request — `ShortenRequest`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `originalUrl` | `String` | `@NotBlank`, `@Pattern(^https?://[^\s]+$)`, `@Size(max=2048)` | Target URL to shorten |
| `customCode` | `String` | Optional, `@Size(min=3, max=20)`, `@Pattern(^[A-Za-z0-9_-]*$)` | Custom alias (auto-generated if absent) |
| `expiresAt` | `LocalDateTime` | Optional, `@Future` | Expiry timestamp |

### Response — `ShortenResponse`

| Field | Type | Description |
|-------|------|-------------|
| `shortCode` | `String` | 6-character Base62 code (or custom alias) |
| `shortUrl` | `String` | Fully qualified redirect URL (e.g. `http://localhost:8091/abc123`) |
| `originalUrl` | `String` | Original URL |
| `createdAt` | `String` | ISO-8601 creation timestamp |
| `expiresAt` | `String` | ISO-8601 expiry timestamp (null if no expiry) |
| `clickCount` | `Long` | Total number of times this link has been followed |

### Response — `UrlStatsResponse`

| Field | Type | Description |
|-------|------|-------------|
| `shortCode` | `String` | Short code |
| `shortUrl` | `String` | Fully qualified redirect URL |
| `originalUrl` | `String` | Original URL |
| `clickCount` | `Long` | Total click count |
| `createdAt` | `String` | Creation timestamp |
| `expiresAt` | `String` | Expiry timestamp |
| `lastClickedAt` | `String` | Timestamp of most recent click |
| `active` | `boolean` | `false` if soft-deleted |
| `expired` | `boolean` | `true` if past `expiresAt` |
| `dailyStats` | `List<DailyClickCount>` | Per-day click counts for the last 7 days (`date` YYYY-MM-DD, `count`) |

### curl examples

**Shorten a URL**
```bash
curl -s -X POST http://localhost:8091/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.example.com/very/long/path"}' | jq .
# → {"shortCode":"x7kQ2m","shortUrl":"http://localhost:8091/x7kQ2m","originalUrl":"...","createdAt":"..."}
```

**Shorten with custom alias and expiry**
```bash
curl -s -X POST http://localhost:8091/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com",
    "customCode": "my-link",
    "expiresAt": "2027-01-01T00:00:00"
  }' | jq .
```

**Redirect (follow with -L)**
```bash
curl -v http://localhost:8091/x7kQ2m
# HTTP/1.1 302 Found
# Location: https://www.example.com/very/long/path
```

**Get click analytics**
```bash
curl -s http://localhost:8091/api/v1/urls/x7kQ2m/stats | jq '{clicks: .clickCount, daily: .dailyStats}'
# → {"clicks":3,"daily":[{"date":"2026-07-29","count":3}]}
```

**Soft-delete**
```bash
curl -s -X DELETE http://localhost:8091/api/v1/urls/x7kQ2m
# 204 No Content — URL deactivated; click history preserved
```

---

## 12. Response Schemas

### `AnalysisResponse` (requirement engine)

```jsonc
{
  "analysisId": "a3f2c1d4-9b8e-4f2a-b1c3-7d6e5f4a3b2c",
  "originalRequirement": "Build a scalable URL shortener service...",
  "scenarioType": "GREENFIELD",
  "scenarioRationale": "Requirement uses construction language ('build', 'scalable', 'implement')",
  "ambiguities": [
    "'scalable' is undefined — specify: expected requests/second, concurrent users, and data volume",
    "Authentication and authorization requirements are not stated"
  ],
  "tasks": [
    {
      "id": "T1",
      "title": "Requirement Analysis & Clarification",
      "description": "Identify all ambiguities, missing constraints, and undefined terms...",
      "category": "CONFIGURATION",
      "priority": "HIGH",
      "dependencies": [],
      "aiPromptHint": "List all ambiguities in this requirement. For each, provide a clarifying question..."
    }
    // T2-T11 follow for GREENFIELD
  ],
  "risks": [
    "DESIGN: Over-engineering early — build for current validated requirements",
    "SECURITY: No authentication specified — public API is a production security risk",
    "AI_HALLUCINATION: AI-generated code may reference non-existent API methods — verify each import",
    "AI_COVERAGE_GAP: AI-generated tests often miss edge cases — add null inputs, boundary values"
  ],
  "assumptions": [
    "Java 21 + Spring Boot 3.x is the target stack",
    "H2 in-memory database is acceptable for the prototype phase",
    "Authentication and authorisation are out of scope for the initial delivery"
  ],
  "analyzedAt": "2026-07-29T14:30:00.123456789"
}
```

### Task counts by scenario

| Scenario | Tasks | Task IDs |
|----------|:-----:|----------|
| GREENFIELD | **11** | T1–T11 |
| BROWNFIELD | **7** | T1–T7 |
| AMBIGUOUS | **6** | T1–T6 |

---

## 13. Classification Logic

`ScenarioClassifierService` applies keyword scoring against the lowercased requirement text using `Set<String>` constants (O(1) per lookup).

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

The sets are `private static final` constants — extending coverage requires a one-line change and a unit test update.

---

## 14. Base62 Encoding

`Base62Encoder` in the `url-shortener` module generates exactly 6-character short codes.

```
alphabet = [A-Za-z0-9]  (62 characters)
62^5  =    916,132,832   minimum value that encodes to exactly 6 chars
62^6  = 56,800,235,583   56 billion combinations

ThreadLocalRandom.nextLong(MIN_6_CHAR, MAX_6_CHAR + 1)
  → always encodes to exactly 6 Base62 characters, no padding needed
  → retries up to 10 times on collision (negligible birthday paradox risk)
```

Click counts are incremented atomically to prevent lost updates under concurrent load:
```java
@Modifying
@Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.id = :id")
void incrementClickCount(@Param("id") Long id);
```

---

## 15. CORS Configuration

Both services configure CORS via `WebMvcConfigurer` (not `@CrossOrigin`, which cannot be applied globally):

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigins)  // supports * wildcard
            .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Location", "Content-Type")
            .allowCredentials(false)                 // MUST be false when pattern is "*"
            .maxAge(3600);
    }
}
```

`allowedOriginPatterns` supports `*` wildcard. `allowCredentials` must be `false` when the origin pattern is `*` — the browser will reject the pre-flight response otherwise.

**Override for production:**
```yaml
cors:
  allowed-origins: "https://your-frontend.com"
```

---

## 16. Design Decisions

| Decision | Alternatives considered | Rationale |
|----------|------------------------|-----------|
| **Keyword scoring for classification** | LLM-based classification | Deterministic, zero-latency, no API key, fully testable with fixed inputs. LLM adds non-determinism and cost; can be layered later. |
| **Multi-module Gradle (core + engine + url-shortener)** | Single monolith; separate repos | Explicit API contracts via `:core`. Separate repos add CI overhead for a prototype. |
| **No persistence in requirement-engine** | Store analyses in PostgreSQL | Analysis is stateless, completes in <5ms. Persistence belongs in a future `analysis-history` module. |
| **Base62 range `[62^5, 62^6)`** | Sequential IDs; UUID prefix; padding | Guarantees exactly 6 characters with no padding logic. 56 billion combinations makes collision rate negligible. |
| **Atomic `@Modifying` click counter** | Read-modify-write in service layer | Prevents lost updates under concurrent load. A single `UPDATE` is atomic at the database level. |
| **Soft-delete (`active=false`)** | Hard DELETE | Preserves click history. Hard delete would cascade-delete analytics data. |
| **Regex `/{shortCode:[A-Za-z0-9_-]{3,20}}`** | Prefix-based routing (`/r/{code}`) | Keeps redirect URLs clean. Regex prevents `/api/...` paths from matching the redirect route. |
| **`allowedOriginPatterns` + `allowCredentials(false)`** | `@CrossOrigin` per controller | Global config prevents per-controller drift. Wildcard + credentials=true is rejected by browsers. |
| **`@WebMvcTest` + `@SpringBootTest` both** | All integration tests; all unit tests | `@WebMvcTest` tests controller layer fast (no DB). `@SpringBootTest @AutoConfigureMockMvc` verifies full stack without following 302 redirects — both layers are needed. |
| **`spring.jpa.open-in-view: false`** | Default (true) | Prevents lazy-loading across request boundaries; eliminates the startup warning in Spring Boot 3. |

---

## 17. Testing

### Coverage summary

| Module | Test class | Type | Count |
|--------|-----------|------|:-----:|
| requirement-engine | `ScenarioClassifierServiceTest` | Unit | 6 |
| requirement-engine | `TaskDecomposerServiceTest` | Unit | 6 |
| requirement-engine | `RequirementAnalyzerServiceTest` | Unit | 8 |
| requirement-engine | `RequirementControllerTest` | `@WebMvcTest` | 9 |
| url-shortener | `UrlShortenerServiceTest` | Unit | 10 |
| url-shortener | `UrlShortenerControllerTest` | `@WebMvcTest` | 10 |
| url-shortener | `UrlShortenerIntegrationTest` | `@SpringBootTest` | 5 |
| **Total** | | | **54** |

### Key testing patterns

**Unit tests** — `@ExtendWith(MockitoExtension.class)`, no Spring context:
```java
@Test
void shorten_withCustomCode_usesCustomCode() {
    when(repository.existsByShortCode("my-alias")).thenReturn(false);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    ShortenResponse result = service.shorten(request("https://x.com", "my-alias"));
    assertThat(result.getShortCode()).isEqualTo("my-alias");
}
```

**Controller tests** — `@WebMvcTest` slice, `@Import(GlobalExceptionHandler.class)`:
```java
@Test
void shorten_missingUrl_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
}
```

**Integration tests** — `@SpringBootTest @AutoConfigureMockMvc`, real H2, full stack, `@AfterEach` cleanup:
```java
@Test
void fullFlow_shortenThenRedirect_clickCountIncremented() throws Exception {
    // 1. POST /api/v1/urls → 201 with shortCode
    // 2. GET /{shortCode}  → 302 (MockMvc does NOT follow redirect)
    //    assert Location header == originalUrl
    // 3. GET /api/v1/urls/{shortCode}/stats → clickCount == 1
}
```

---

## 18. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|:----------:|:------:|------------|
| Keyword scorer misclassifies mixed-language requirements | Medium | Medium | AMBIGUOUS fallback on tied scores; per-request rationale allows engineer override |
| **AI_HALLUCINATION**: LLM-generated code references non-existent API methods or packages | High | Medium | Every analysis response surfaces this risk; engineer must verify each import against Javadoc before committing |
| **AI_COVERAGE_GAP**: LLM-generated tests miss edge cases (null inputs, boundary values, exception paths) | High | Medium | Every analysis response surfaces this risk; engineer must review each generated test method |
| LLM output accepted without review — engineer doesn't own quality | Medium | High | Prototype documents 4 cases where engineer review caught and fixed LLM output issues |
| Requirement text contains PII or credentials | Low | High | Engine is stateless; logs only character counts, never requirement text; deploy behind API gateway with TLS |
| `url-shortener` ships with H2 in production | Low | High | Production must override `SPRING_DATASOURCE_URL` to PostgreSQL; H2 console enabled only in default profile |
| Base62 collision under high load | Very Low | Low | 56 billion combinations; `ThreadLocalRandom`; up to 10 retries on collision |
| Click count lost under concurrent redirects | Low | Medium | Atomic `@Modifying` UPDATE prevents lost updates |
| CORS misconfiguration in production | Low | High | `cors.allowed-origins` externalised via config; wildcard only for dev; `allowCredentials=false` enforced |
| Docker image runs as root | Low | High | `Dockerfile` creates `appuser`/`appgroup`; sets `USER appuser` before `ENTRYPOINT` |
| Gradle build cache serves stale compiled classes | Low | Low | `./gradlew clean build` for fresh output; CI should use `--no-build-cache` on release builds |
| System `gradle` binary (9.x) used instead of wrapper | Medium | High | Always use `./gradlew`; `gradle.properties` pins JVM via `org.gradle.java.home` |

---

## 19. Extending the System

Adding a new module (e.g. `notification-engine`) takes four steps:

**Step 1** — Register in `settings.gradle.kts`:
```kotlin
include("core", "requirement-engine", "url-shortener", "notification-engine")
```

**Step 2** — Create `notification-engine/build.gradle.kts` by copying `url-shortener/build.gradle.kts` and adjusting dependencies. The `:core` dependency gives the new module immediate access to all shared domain types.

**Step 3** — Assign a port following the sequential convention:

| Module | Port |
|--------|------|
| requirement-engine | 8090 |
| url-shortener | 8091 |
| notification-engine | 8092 |
| code-generator-engine | 8093 |

**Step 4** — Add a service block to `docker-compose.yml`:
```yaml
  notification-engine:
    build:
      context: .
      dockerfile: notification-engine/Dockerfile
    ports:
      - "8092:8092"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8092/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    restart: unless-stopped
```

---

## 20. Assumptions & Limitations

### Assumptions

- Requirements are written in English. The keyword scorer does not support other languages.
- The caller is a trusted internal client. No authentication or rate-limiting is implemented; these belong at the API gateway layer.
- A "requirement" is a single coherent statement (10–5000 characters). Multi-requirement batch processing is out of scope.
- H2 in-memory storage is acceptable for the `url-shortener` prototype. All data is lost on restart. For persistent deployments, override `SPRING_DATASOURCE_URL` with a PostgreSQL connection string.
- Click analytics are grouped by day in Java code for H2 compatibility. A PostgreSQL deployment could use a `DATE_TRUNC` query instead.

### Limitations

- **No persistence in requirement-engine**: Each analysis is stateless. Add an `analysis-history` module to store results.
- **No authentication**: Neither service authenticates callers. Do not expose either directly on the public internet without an API gateway.
- **Keyword classifier only**: Classification depends entirely on keyword sets. Requirements with domain-specific vocabulary may be misclassified. The AMBIGUOUS path is the safety net.
- **Fixed task count per scenario**: GREENFIELD always produces 11 tasks, BROWNFIELD 7, AMBIGUOUS 6. There is no dynamic task generation based on requirement complexity.
- **`aiPromptHint` is a template, not an execution**: The hints embed the requirement text but do not call any LLM. They must be pasted manually into an LLM by the engineer.
- **H2 only for url-shortener**: To switch to PostgreSQL, provide `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables and remove `spring.datasource.driver-class-name: org.h2.Driver`.
- **No redirect analytics beyond click count per day**: The `click_events` table captures `user_agent`, `ip_address`, and `referer`, but the `/stats` endpoint only aggregates by day. Geolocation, browser, and referrer breakdowns are not implemented.
- **IP tracking is not proxy-aware**: Click events record `request.getRemoteAddr()`, which returns the direct TCP client IP. Behind a load balancer or reverse proxy this is the proxy IP, not the real client IP. Production deployments should read `X-Forwarded-For` header instead.
- **Click tracking is synchronous**: The redirect and click event recording share a single `@Transactional` boundary. The redirect blocks until the DB write completes. For high-throughput production use, click recording should be moved to an `@Async` fire-and-forget call.
- **English only**: All keyword matching and fixed rationale strings are in English.
