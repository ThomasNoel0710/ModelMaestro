# ModelMaestro

ModelMaestro is a multi-model AI orchestration platform. A capable supervisor model plans and reviews work while lower-cost worker models execute suitable subtasks.

## Repository structure

```text
ModelMaestro/
|- backend/    Spring Boot API and orchestration runtime
|- frontend/   Web client placeholder
`- compose.yml Local PostgreSQL service
```

## Current foundation

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper
- Spring MVC and Bean Validation
- Spring Data JPA
- PostgreSQL and Flyway migrations
- Actuator health checks
- H2-backed application context tests

## Run locally

Start PostgreSQL from the repository root:

```bash
docker compose up -d
```

Start the backend on Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The initial endpoints are:

- `GET http://localhost:8080/api/v1/system`
- `GET http://localhost:8080/actuator/health`

## Design boundary

ModelMaestro will keep deterministic controls (budget, concurrency, retries, permissions, and task state) in application code. Models will handle semantic decisions such as planning, delegation, review, and synthesis.

