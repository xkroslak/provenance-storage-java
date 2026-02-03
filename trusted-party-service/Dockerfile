# Build
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY distributed-prov-system-service/pom.xml distributed-prov-system-service/pom.xml
COPY trusted-party-service/pom.xml trusted-party-service/pom.xml

COPY trusted-party-service/src trusted-party-service/src

RUN mvn -pl trusted-party-service -am -DskipTests package

# Run
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/trusted-party-service/target/*.jar /app/app.jar

EXPOSE 8020

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
