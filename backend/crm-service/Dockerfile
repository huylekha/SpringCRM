FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy everything for Maven multi-module build
COPY . .

# Build the specific service
RUN mvn -B -pl shared-lib,crm-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy the built jar
COPY --from=build /app/crm-service/target/*.jar app.jar

# Create logs directory with proper permissions
RUN mkdir -p /logs && chown app:app /logs

USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
