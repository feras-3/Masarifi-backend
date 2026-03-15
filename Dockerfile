# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy dependency manifests first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build, skipping tests
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create directory for the SQLite database file
RUN mkdir -p /app/data

# Copy the built jar from the builder stage
COPY --from=builder /build/target/expense-tracker-backend-*.jar app.jar

# Expose the application port
EXPOSE 8765

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
