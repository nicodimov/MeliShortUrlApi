# Build stage
FROM maven:3.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package spring-boot:repackage -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/MeliShortUrlApi-0.0.1-SNAPSHOT.jar app.jar
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# Expose the port your app runs on
EXPOSE 8080

# Use the agent in your ENTRYPOINT
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-Dotel.exporter.otlp.protocol=grpc", "-Dotel.exporter.otlp.endpoint=http://otel-collector:4317", "-Dotel.metrics.exporter=otlp", "-Dotel.traces.exporter=otlp", "-Dotel.logs.exporter=none", "-jar", "app.jar", "--spring.profiles.active=docker"] 