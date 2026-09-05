# =============================================================
# Build
# =============================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies are resolved in their own layer so a source-only
# change does not re-download the world on every deploy.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B -DskipTests package


# =============================================================
# Runtime
# =============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build \
     /workspace/target/warehouse-dispatch-gpms-backend-0.0.1-SNAPSHOT.jar \
     app.jar

# Documentation only - the real port comes from $PORT at runtime.
EXPOSE 8080

# Run as a non-root user.
RUN useradd --system --uid 10001 gpms \
    && chown -R gpms:gpms /app
USER gpms

# Render's free tier gives 512MB. The JVM's default heap sizing
# reads the HOST's memory on some setups and happily oversubscribes,
# which shows up as the container being OOM-killed mid-startup.
# MaxRAMPercentage keeps the heap inside the container's real limit.
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=70", \
            "-XX:+UseSerialGC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]
