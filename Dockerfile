
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw || true
RUN ./mvnw -DskipTests dependency:go-offline || mvn -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -DskipTests package || mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
USER spring:spring

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["java","-jar","/app/app.jar"]
