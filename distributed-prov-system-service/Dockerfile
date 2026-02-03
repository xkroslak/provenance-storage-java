# Build
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY distributed-prov-system-service/pom.xml distributed-prov-system-service/pom.xml
COPY trusted-party-service/pom.xml trusted-party-service/pom.xml

COPY distributed-prov-system-service/src distributed-prov-system-service/src

RUN mvn -pl distributed-prov-system-service -am -DskipTests package

# Run
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/distributed-prov-system-service/target/*.jar /app/app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
