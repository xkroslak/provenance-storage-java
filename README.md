# Provenance Storage (Java)

Multi-module Maven project for the provenance storage system and trusted party service.

## Modules
- **distributed-prov-system-service**: Provenance storage service (Spring Boot)
- **trusted-party-service**: Trusted party service (Spring Boot)

## Requirements
- Java 21+
- Maven 3.9+
- Neo4j (for distributed-prov-system-service)
- PostgreSQL (for trusted-party-service)

## Local configuration (recommended)
Local secrets should **not** be committed. Use the local profile files that are ignored by Git:

- distributed-prov-system-service/src/main/resources/application-local.yaml
- trusted-party-service/src/main/resources/application-local.yaml

Run with the `local` profile:

- IntelliJ: set environment variable `SPRING_PROFILES_ACTIVE=local`
- CLI: `--spring.profiles.active=local`

## Environment variables (alternative)
You can also supply values via environment variables instead of a local profile. See:

- distributed-prov-system-service/src/main/resources/application.yaml
- trusted-party-service/src/main/resources/application.yaml

## Running (Maven)
From the repo root:

- `./mvnw -pl distributed-prov-system-service spring-boot:run`
- `./mvnw -pl trusted-party-service spring-boot:run`

Add `-Dspring-boot.run.profiles=local` if using local profile.

## Docker (Compose)
Ensure you have local certificates in `./certs`:

- `certs/cert-dev/cert.pem`
- `certs/cert-dev/ec_key.pem`
- `certs/trusted_certs/`

Then run:

- `docker compose up`

After startup, the APIs are available at:

- Swagger UI (Trusted Party): http://localhost:8020/swagger-ui/index.html
- Swagger UI (Provenance Storage): http://localhost:8000/swagger-ui/index.html

Monitoring (Docker Compose):

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Actuator & Metrics
Both services expose Actuator endpoints. These are useful for health checks and Prometheus scraping:

- Health: http://localhost:8020/actuator/health and http://localhost:8000/actuator/health
- Metrics list: http://localhost:8020/actuator/metrics and http://localhost:8000/actuator/metrics
- Prometheus: http://localhost:8020/actuator/prometheus and http://localhost:8000/actuator/prometheus

## Security
Do **not** commit private keys. Keep certificates and local secrets outside Git or in ignored local profile files.
