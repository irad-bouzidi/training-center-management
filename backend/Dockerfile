# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Dev stage: hot-reload via `mvn spring-boot:run` + devtools, source
# bind-mounted over this at runtime (see docker-compose.override.yml.example).
# Not used in production - selected explicitly via `build.target: dev`.
FROM maven:3.9-eclipse-temurin-21 AS dev
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

CMD ["mvn", "spring-boot:run"]

# --- Runtime stage (default target) ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /build/target/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
