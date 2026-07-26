# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Resolve dependencies first to leverage Docker layer caching
COPY pom.xml .
RUN mvn -q dependency:go-offline || true

# Build the application (skip tests in image build)
COPY src ./src
RUN mvn -q package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
