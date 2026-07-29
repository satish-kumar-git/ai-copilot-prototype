# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM gradle:8.10-jdk21 AS builder

WORKDIR /workspace

# Copy dependency descriptors first so this layer is cached until they change
COPY gradle gradle/
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY core/build.gradle.kts core/build.gradle.kts
COPY url-shortener/build.gradle.kts url-shortener/build.gradle.kts

RUN ./gradlew dependencies --no-daemon

# Copy source and build the fat jar
COPY core/src core/src/
COPY url-shortener/src url-shortener/src/

RUN ./gradlew :url-shortener:bootJar --no-daemon -x test

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup \
    /workspace/url-shortener/build/libs/url-shortener.jar app.jar

EXPOSE 8091

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
