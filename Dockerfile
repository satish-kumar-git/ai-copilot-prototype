# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM gradle:8.10-jdk21 AS builder

WORKDIR /workspace

# Copy dependency descriptors first so this layer is cached until they change
COPY gradle gradle/
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY core/build.gradle.kts core/build.gradle.kts
COPY requirement-engine/build.gradle.kts requirement-engine/build.gradle.kts

RUN ./gradlew dependencies --no-daemon

# Copy source and build the fat jar
COPY core/src core/src/
COPY requirement-engine/src requirement-engine/src/

RUN ./gradlew :requirement-engine:bootJar --no-daemon -x test

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup \
    /workspace/requirement-engine/build/libs/requirement-engine.jar app.jar

EXPOSE 8090

USER appuser

ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-jar", "app.jar"]
