# syntax=docker/dockerfile:1
# ---------- Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests package && cp target/BarberShop-Apis-*.jar app.jar

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Utilisateur non-root
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 --create-home app && mkdir -p /app/uploads /app/logs && chown -R app /app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
# Secrets fournis par variables d'environnement (DB_PASSWORD, JWT_SECRET_BASE64, MAIL_*, ...)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
