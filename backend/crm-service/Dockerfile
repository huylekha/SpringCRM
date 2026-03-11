FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY ../pom.xml ../pom.xml
COPY ../shared-lib ../shared-lib
COPY pom.xml .
COPY src ./src
RUN cd .. && mvn -B -pl shared-lib,crm-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
