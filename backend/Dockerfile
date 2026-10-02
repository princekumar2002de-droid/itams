# =============================================================================
#  ITAMS backend — production Docker image
#  Multi-stage: Maven build in an Eclipse Temurin JDK, then run the jar
#  on a small JRE image with a non-root user.
# =============================================================================

# ---------- Stage 1: build ---------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

# Maven is the only tool we need on top of the JDK.
RUN apk add --no-cache maven

WORKDIR /workspace

# Cache dependencies in a separate layer — pom.xml rarely changes.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Skip tests in the image build; CI runs them separately.
RUN mvn -B -q -DskipTests package \
 && cp target/*.jar /workspace/app.jar

# ---------- Stage 2: runtime -------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Non-root user. Any exploit that gains code exec runs as `itams`, not root.
RUN addgroup -S itams && adduser -S -G itams itams

# curl is used only by the HEALTHCHECK below (Alpine has no curl by default).
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
RUN chown -R itams:itams /app
USER itams

# The prod profile expects JWT_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD from
# the environment (docker-compose sets them via .env). The app fails to start
# if JWT_SECRET is unset — that's deliberate.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080

# Actuator health endpoint is public — good citizen for orchestrators.
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# `exec` so the JVM is PID 1 and receives SIGTERM directly from Docker.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
